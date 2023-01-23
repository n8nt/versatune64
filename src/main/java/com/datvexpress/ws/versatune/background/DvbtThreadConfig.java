package com.datvexpress.ws.versatune.background;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration

public class DvbtThreadConfig {

    @Bean(name = "threadPoolExecutor")
    public TaskExecutor threadPoolTaskExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(64);
        executor.setThreadNamePrefix("DVBT_task-");
        executor.setBeanName("Dvbt-task-executor");
        executor.initialize();

        return executor;
    }
}
