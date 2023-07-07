package com.tahauddin.syed.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchJobService {

    private final Job job;
    private final JobLauncher jobLauncher;

    public void runJob() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        JobParameters jobParameters = new JobParametersBuilder()
                .addDate("todaysDate", Date.from(Instant.now()))
                .addString("inputPath", "Sales.csv")
                .addString("multipleWriter", "INSERT & UPDATE")
                .toJobParameters();
        List<Throwable> failureExceptions = jobLauncher.run(job, jobParameters).getFailureExceptions();
        failureExceptions.forEach(l -> log.info("Failure Exceptions are :: {}", l.getMessage()));
    }

}
