package com.tahauddin.syed.runner;

import com.tahauddin.syed.service.BatchJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MyJobRunner implements CommandLineRunner {

    private final BatchJobService batchJobService;


    @Override
    public void run(String... args) throws Exception {
        batchJobService.runJob();
    }
}
