package com.tahauddin.syed.config.batch;

import com.tahauddin.syed.config.listener.MyStepExecutionListener;
import com.tahauddin.syed.model.Sales;
import com.tahauddin.syed.model.SalesEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.annotation.OnSkipInRead;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.*;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;


@Configuration
@Slf4j
public class BatchConfiguration {

    private static final String SALES_INSERT_QUERY = "INSERT INTO SALES_TABLE" +
            "(ID, " +
            "REGION," +
            "COUNTRY," +
            "ITEM_TYPE," +
            "SALES_CHANNEL," +
            "ORDER_PRIORITY," +
            "ORDER_DATE," +
            "ORDER_ID," +
            "SHIP_DATE," +
            "UNITS_SOLD," +
            "UNIT_PRICE," +
            "UNIT_COST," +
            "TOTAL_REVENUE," +
            "TOTAL_COST," +
            "TOTAL_PROFIT" +
            ") " +
            "VALUES (" +
            ":id," +
            ":region," +
            ":country," +
            ":itemType," +
            ":saleChannel," +
            ":orderPriority," +
            ":orderDate," +
            ":orderID," +
            ":shipDate," +
            ":unitsSold," +
            ":unitPrice," +
            ":unitCost," +
            ":totalRevenue," +
            ":totalCost," +
            ":totalProfit" +
            ")";
    private static final String SALES_UPDATE_QUERY = "UPDATE SALES_TABLE SET " +
            "COUNTRY = :country " +
            //      "WHERE " +
            //    "ID = :id" +
            " ";


    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final Resource salesResource;
    private final DataSource dataSource;
    private MyStepExecutionListener stepExecutionListener;

    public BatchConfiguration(JobRepository jobRepository,
                              PlatformTransactionManager platformTransactionManager,
                              @Value("classpath:Sales.csv")
                              Resource salesResource,
                              DataSource dataSource) {
        this.jobRepository = jobRepository;
        this.platformTransactionManager = platformTransactionManager;
        this.salesResource = salesResource;
        this.dataSource = dataSource;
    }

    @Bean
    public JobBuilder jobBuilder() {
        return new JobBuilder("jobOne", jobRepository);
    }

    @Bean
    public StepBuilder stepBuilder() {
        return new StepBuilder("stepOne", jobRepository);
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(){
        return new JdbcTemplate(dataSource);
    }


    @Bean
    public Job job() {
        return jobBuilder()
                .incrementer(new RunIdIncrementer())
//                .start(taskletStep())
//                .next(chunkBasedStep())
                .start(chunkBasedCsvStep())
                //     .preventRestart()
                .build();
    }

    @Bean
    public Step taskletStep() {
        return stepBuilder()
                .tasklet((contribution, chunkContext) -> {
                    //           log.info("In Tasklet Step :: ");
                    return RepeatStatus.FINISHED;
                })
                .transactionManager(platformTransactionManager)
                .build();
    }

    @Bean
    public Step chunkBasedStep() {
        return stepBuilder()
                .<Sales, Sales>chunk(5, platformTransactionManager)
                .reader(reader())
                .processor(processor())
                .writer(writer())
                .transactionManager(platformTransactionManager)
     //           .listener(stepExecutionListener)
                .build();
    }

    @Bean
    public Step chunkBasedCsvStep() {
        return stepBuilder()
                .<Sales, Sales>chunk(5000, platformTransactionManager)
                .reader(csvSalesReader(null))
                .processor((ItemProcessor) asyncItemProcessor())
                .writer(salesAsyncItemWriter())
                //        .writer(jdbcBatchSalesUpdateWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(Integer.MAX_VALUE)
                .skipPolicy(new AlwaysSkipItemSkipPolicy())
                .transactionManager(platformTransactionManager)
                .build();
    }

    @Bean
    public ItemWriter<Sales> jdbcBatchSalesWriter() {
        JdbcBatchItemWriter<Sales> itemWriter = null;
        try {
            itemWriter = new JdbcBatchItemWriter<>();
            log.info("IN JDBC Batch Item Writer for INSERT");
            itemWriter.setJdbcTemplate(namedParameterJdbcTemplate(dataSource));
            itemWriter.setSql(SALES_INSERT_QUERY);
            BeanPropertyItemSqlParameterSourceProvider<Sales> parameterSourceProvider = new BeanPropertyItemSqlParameterSourceProvider<>();
            itemWriter.setItemSqlParameterSourceProvider(parameterSourceProvider);
            log.info("Writing Files to DB ::", itemWriter);
        } catch (Exception e) {
        }
        return itemWriter;
    }

    @Bean
    public AsyncItemWriter<Sales> salesAsyncItemWriter() {
        AsyncItemWriter<Sales> salesAsyncItemWriter = new AsyncItemWriter<>();
        salesAsyncItemWriter.setDelegate(jdbcBatchSalesWriter());
    //    salesAsyncItemWriter.setDelegate(jdbcBatchSalesUpdateWriter());
        return salesAsyncItemWriter;
    }

    @Bean
    public ItemWriter<Sales> jdbcBatchSalesUpdateWriter() {
        JdbcBatchItemWriter<Sales> itemWriter = new JdbcBatchItemWriter<>();
        log.info("IN JDBC Batch Item Writer for UPDATE");
//        itemWriter.setAssertUpdates(false);
        itemWriter.setDataSource(dataSource);
//        itemWriter.setJdbcTemplate(namedParameterJdbcTemplate(dataSource));
        itemWriter.setSql(SALES_UPDATE_QUERY);
//        itemWriter.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<Sales>());
        itemWriter.setItemPreparedStatementSetter((item, ps) -> {
            ps.setString(0, "USA");
            //        ps.setString(1, item.getId());
            //        ps.execute();
        });
        itemWriter.afterPropertiesSet();
        return itemWriter;

        /*return chunk -> {
       //     SqlParameterSource[] sqlParameterSources = SqlParameterSourceUtils.createBatch(chunk);
       //     namedParameterJdbcTemplate.batchUpdate(SALES_UPDATE_QUERY,sqlParameterSources);
            jdbcTemplate.setDataSource(dataSource);
            int[] batchUpdate = jdbcTemplate.batchUpdate(SALES_UPDATE_QUERY, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
               //     log.info("Inside Prepared Statement Setting Values :: ");
            //        log.info("Inside Prepared Statement  :: {}",chunk.getItems().get(i).getCountry());
                    log.info("Inside Prepared Statement Setting Country to  :: USA" );
            //        ps.setString(1, chunk.getItems().get(i).getCountry());
                    ps.setString(1, "USA");
                    ps.addBatch();
                }
                @Override
                public int getBatchSize() {
                    return chunk.size();
                }
            });
        };*/

    //    return null;
    }

    /**
     * Adding the value at run time and passing the input path to the reader
     *
     * @param resource
     * @return
     */
    @Bean
    @StepScope
    public FlatFileItemReader<Sales> csvSalesReader(@Value("#{jobParameters['inputPath']}") ClassPathResource resource) {

        DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();
        delimitedLineTokenizer.setNames("Region,Country,ItemType,SalesChannel,OrderPriority,OrderDate,OrderID,ShipDate,UnitsSold,UnitPrice,UnitCost,TotalRevenue,TotalCost,TotalProfit".split(","));

        BeanWrapperFieldSetMapper<Sales> salesBeanWrapperFieldSetMapper = new BeanWrapperFieldSetMapper<>();
        salesBeanWrapperFieldSetMapper.setTargetType(Sales.class);

        return new FlatFileItemReaderBuilder<Sales>()
                .name("Sales Csv Reader")
                .fieldSetMapper(salesBeanWrapperFieldSetMapper)
                .linesToSkip(1)
                .lineTokenizer(delimitedLineTokenizer)
                .resource(resource)
                .build();
    }

    public ItemWriter<Sales> writer() {
        return chunk -> {
            log.info("In Writer size is :: {}", chunk.getItems().size());
        };
    }

    @Bean
    public ItemProcessor<Sales, Sales> processor() {
        return item -> {
//            Thread.sleep(5000);
            item.setId(UUID.randomUUID().toString());
            return item;
        };
    }

    public AsyncItemProcessor<Sales, Sales> asyncItemProcessor() {
        AsyncItemProcessor<Sales, Sales> salesSalesAsyncItemProcessor = new AsyncItemProcessor<>();
        salesSalesAsyncItemProcessor.setDelegate(processor());
        salesSalesAsyncItemProcessor.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return salesSalesAsyncItemProcessor;
    }


    public ItemReader<Sales> reader() {
        return () -> Sales.builder().country("Qatar").build();
    }


    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadGroupName("Sales Thread Group");
        threadPoolTaskExecutor.setQueueCapacity(10);
        threadPoolTaskExecutor.setCorePoolSize(5);
        threadPoolTaskExecutor.setMaxPoolSize(5);
        threadPoolTaskExecutor.setThreadNamePrefix("Sales Info ->  ");
        threadPoolTaskExecutor.afterPropertiesSet();
        return threadPoolTaskExecutor;
    }

}
