package com.tahauddin.syed;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableBatchProcessing(isolationLevelForCreate = "ISOLATION_READ_UNCOMMITTED")
public class Batch04Application {

	public static void main(String[] args) {
		SpringApplication.run(Batch04Application.class, args);
	}

}
