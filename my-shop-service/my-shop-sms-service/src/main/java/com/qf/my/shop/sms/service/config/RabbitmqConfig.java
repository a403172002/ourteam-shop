package com.qf.my.shop.sms.service.config;

import com.qf.constant.RabbitConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitmqConfig {

    @Bean
    public Queue getQueue(){
        return new Queue(RabbitConstant.SMS_QUEUE);
    }
    @Bean
    public TopicExchange getExchange(){
        return new TopicExchange(RabbitConstant.SMS_TOPIC_EXCHANGE);
    }
    @Bean
    public Binding getBinding(Queue queue,TopicExchange topicExchange){
        return BindingBuilder.bind(queue).to(topicExchange).with("sms.send");
    }

}
