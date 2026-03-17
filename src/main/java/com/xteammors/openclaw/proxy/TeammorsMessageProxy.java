package com.xteammors.openclaw.proxy;

import com.alibaba.fastjson.JSONObject;
import com.xteammors.openclaw.adapter.TeammorsMessageAdapter;
import com.xteammors.openclaw.rag.service.RAGService;
import com.xteammors.openclaw.skills.base.AgentSkill;
import com.xteammors.openclaw.utils.JsonUtils;
import com.xteammors.openclaw.utils.ThreadUtils;
import com.xteammors.openclaw.wssdk.XMessageObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
public class TeammorsMessageProxy implements XMessageObserver {

    @Autowired
    RAGService ragService;

    @Autowired
    List<AgentSkill> skills;


    @Override
    public void onIMMessage(String message) {

        try {


            if(JsonUtils.isJsonObject(message)) {

                JSONObject jsonObject = JSONObject.parseObject(message);
                if(jsonObject.containsKey("dataBody")) {
                    String dataBody = jsonObject.getString("dataBody");
                    if(JsonUtils.isJsonObject(dataBody)) {

                        JSONObject dataBodyJson = JSONObject.parseObject(dataBody);
                        if (dataBodyJson.containsKey("text")) {

                            log.info("dataBodyJson:{}", dataBodyJson);

                            String chatId = dataBodyJson.getString("chatId");
                            String text = dataBodyJson.getString("text");

                            TeammorsMessageAdapter teammorsMessageAdapter = new TeammorsMessageAdapter(chatId, text, ragService, skills);
                            ThreadUtils.instance().getExecutor().execute(teammorsMessageAdapter);

                        }
                    }
                }

            }

        } catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void onIMError(String message) {
        log.error("message:"+message);
    }



}