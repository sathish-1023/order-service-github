package com.mrParashurama.Order_Service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("Missing")
@Configuration
public class KafkaConfig {
    public NewTopic createTopic(){
        return new NewTopic("notificationTopic",3, (short) 1);
    }
}
