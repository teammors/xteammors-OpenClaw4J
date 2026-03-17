package com.xteammors.openclaw.manager;

import com.xteammors.openclaw.comm.CommParameters;
import com.xteammors.openclaw.property.RedisProperty;
import com.xteammors.openclaw.property.TeammorsBotProperty;
import com.xteammors.openclaw.property.TelegramBotProperty;
import com.xteammors.openclaw.proxy.TeammorsMessageProxy;
import com.xteammors.openclaw.proxy.TelegramMessageProxy;
import com.xteammors.openclaw.utils.RedisUtils;
import com.xteammors.openclaw.wssdk.XMessageClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


@Component
public class AgentManager {

    @Autowired
    RedisProperty redisProperty;

    @Autowired
    TelegramBotProperty telegramBotProperty;

    @Autowired
    TeammorsBotProperty teammorsBotProperty;

    @Autowired
    TelegramMessageProxy telegramMessageProxy;

    @Autowired
    TeammorsMessageProxy teammorsMessageProxy;


    public void startAgent(){

        CommParameters.instance().setTeammorsBotToken(teammorsBotProperty.getToken());
        CommParameters.instance().setTelegramBotId(telegramBotProperty.getId());
        CommParameters.instance().setTelegramBotToken(telegramBotProperty.getToken());
        CommParameters.instance().setTelegramBotName(telegramBotProperty.getName());

        try {

            RedisUtils.instance().init(redisProperty.getIp(),redisProperty.getUser(),
                    redisProperty.getPassword(),redisProperty.getPort(),
                    redisProperty.getDb(), redisProperty.getCluster());

            XMessageClient.instance().addObserver(teammorsMessageProxy);
            XMessageClient.instance().init(teammorsBotProperty.getToken());
            System.out.println("TeammorsBot 启动成功!");

            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramMessageProxy);
            System.out.println("TelegramBot 启动成功!");

        }catch (Exception e){
            e.printStackTrace();
        }


    }

}
