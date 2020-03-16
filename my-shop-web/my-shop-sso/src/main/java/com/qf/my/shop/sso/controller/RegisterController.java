package com.qf.my.shop.sso.controller;

import com.qf.constant.RabbitConstant;
import com.qf.dto.ResultBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("user")
public class RegisterController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送短信验证码
     * @param mobile 手机号
     * @return 结果
     */
    @RequestMapping("getCode")
    @ResponseBody
    public ResultBean getCode(String mobile){
        ////使用mq让第三方短信服务商来发送短信
//        String content = String.valueOf((int) (((Math.random() * 9 + 1) * 1000)));
//        Map<String,Object> map = new HashMap<String, Object>();
//        String extno = "";
//        String tpId = "02c6342fb5d64b038c740a40aec0fb70";
//        map.put("content",content);
//        map.put("mobile",mobile);
//        map.put("extno",extno);
//        map.put("tpId",tpId);
        rabbitTemplate.convertAndSend(RabbitConstant.SMS_TOPIC_EXCHANGE,"sms.send",mobile);
        return ResultBean.success();
    }

}
