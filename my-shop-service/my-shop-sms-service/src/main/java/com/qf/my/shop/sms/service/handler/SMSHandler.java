package com.qf.my.shop.sms.service.handler;

import com.qf.constant.RabbitConstant;
import com.qf.constant.RedisConstant;
import com.qf.util.StringUtil;
import com.rabbitmq.client.Channel;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.util.DigestUtils.md5Digest;

@Component
public class SMSHandler {
    /**
     *	账号
     */
    static String ACCOUNT_SID = "ZH000001278";

    /**
     * APIKEY
     */
    static String ACCOUNT_APIKEY = "12852f97-7fa4-4913-8367-2cb6a9603297";

    /**
     * utf8编码
     */
    static final String CHARSET_UTF8 = "utf-8";

    static final String GB2312 = "GB2312";

    /**
     * HttpUrl
     */
    static String HttpUrl = "http://api.rcscloud.cn:8030/rcsapi/rest";  //或使用ip:  http://121.41.114.153:8030/rcsapi/rest

    static String tplId = "02c6342fb5d64b038c740a40aec0fb70";

    @Autowired
    private RedisTemplate redisTemplate;

//    @RabbitListener(queues = RabbitConstant.SMS_QUEUE)
//    public static void main(String[] args,String mobile) {
//        String content = String.valueOf((int) (((Math.random() * 9 + 1) * 1000)));
//        sendTplSms("02c6342fb5d64b038c740a40aec0fb70",mobile, "@1@="+content, "");
//    }



    /**
     * 发送模板短信
     * @param message 手机号码
     * @return json字符串,详细描述请参考接口文档
     *
     * String
     */
    @RabbitListener(queues = RabbitConstant.SMS_QUEUE)
    public String sendTplSms(Message message,Channel channel) throws IOException {
//        System.out.println(mobile);
//        return  null;
//    }
        DefaultHttpClient httpclient = new DefaultHttpClient();
        String resultJson = "";
        String content = "@1@=" +  String.valueOf((int) (((Math.random() * 9 + 1) * 1000)));
        String mobile = new String(message.getBody());
        try {
            //签名:Md5(sid+key+tplid+mobile+content)
            StringBuilder signStr = new StringBuilder();
            signStr.append(ACCOUNT_SID).append(ACCOUNT_APIKEY).append(tplId).append(mobile).append(content);
            /**
             * 注意：发送短信返回1005错误码时，主要由于中文字符转码错误造成签名鉴权失败
             * 在实际开发中采用采用UTF-8或者GB2312转码
             * **/
            //String sign = md5Digest(changeCharset(signStr.toString(), "GB2312"));
            String sign = md5Digest(changeCharset(signStr.toString(), "UTF-8"));


            //创建HttpPost请求
            HttpPost httppost = new HttpPost(HttpUrl +"/sms/sendtplsms.json");//?sid="+ACCOUNT_SID+"&sign="+sign
            //构建form
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("sid", ACCOUNT_SID));
            nvps.add(new BasicNameValuePair("sign", sign));
            nvps.add(new BasicNameValuePair("tplid", tplId));
            nvps.add(new BasicNameValuePair("mobile", mobile));
            nvps.add(new BasicNameValuePair("content", content));
            nvps.add(new BasicNameValuePair("extno", 	""));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nvps,CHARSET_UTF8);
            httppost.setEntity(entity);

            //设置请求表头信息，POST请求必须采用application/x-www-form-urlencoded否则提示415错误
            httppost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httppost.setHeader("Content-Encoding", CHARSET_UTF8);
            //执行请求
            HttpResponse response = httpclient.execute(httppost);
            //获取响应Entity
            HttpEntity httpEntity = response.getEntity();
            //返回JSON字符串格式，用户根据实际业务进行解析处理
            if (httpEntity != null)
                resultJson = EntityUtils.toString(httpEntity, CHARSET_UTF8);

            EntityUtils.consume(entity);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }  finally {
            if (httpclient != null)
                httpclient.getConnectionManager().shutdown();
        }
        System.out.println("resultJson=" +resultJson);
        String redisKey = StringUtil.getRedisKey(RedisConstant.REGISTER_PHONE,mobile);
        redisTemplate.opsForValue().set(redisKey,content,5, TimeUnit.MINUTES);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        return resultJson;
    }
    /**
     * MD5算法
     * @param src
     * @return
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * String
     */
    public static String md5Digest(String src) throws NoSuchAlgorithmException, UnsupportedEncodingException{
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] b = md.digest(src.getBytes(CHARSET_UTF8));
        return byte2HexStr(b);
    }

    private static String byte2HexStr(byte[] b){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; ++i) {
            String s = Integer.toHexString(b[i] & 0xFF);
            if (s.length() == 1)
                sb.append("0");
            sb.append(s.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * 字符编码转换
     * @param str
     * @param newCharset
     * @return
     * @throws UnsupportedEncodingException
     * String
     */
    public static String changeCharset(String str, String newCharset)
            throws UnsupportedEncodingException {
        if (str != null) {
            //用默认字符编码解码字符串。
            byte[] bs = str.getBytes();
            //用新的字符编码生成字符串
            return new String(bs, newCharset);
        }
        return null;
    }
}
