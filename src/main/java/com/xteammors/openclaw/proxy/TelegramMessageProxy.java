package com.xteammors.openclaw.proxy;

import com.xteammors.openclaw.adapter.TelegramMessageAdapter;
import com.xteammors.openclaw.comm.CommParameters;
import com.xteammors.openclaw.rag.service.RAGService;
import com.xteammors.openclaw.skills.base.AgentSkill;
import com.xteammors.openclaw.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;


@Slf4j
@Service
public class TelegramMessageProxy extends TelegramLongPollingBot {

    @Autowired
    RAGService ragService;

    @Autowired
    List<AgentSkill> skills;

    @Override
    public String getBotUsername() {
        // 示例：return "MyTestJavaBot";
        return CommParameters.instance().getTelegramBotName(); // 替换这里 ↓
    }

    @Override
    public String getBotToken() {
        // 示例：return "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11";
        return CommParameters.instance().getTelegramBotId()+":"+CommParameters.instance().getTelegramBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {

        // 判空：确保更新包含消息，且消息有文本内容
        if (update.hasMessage() && update.getMessage().hasText()) {
            // 获取用户消息的核心信息
            Long chatId = update.getMessage().getChatId(); // 聊天ID（唯一标识用户/群聊，用于回复）
            String userInput = update.getMessage().getText(); // 用户发送的文本内容
            String userName = update.getMessage().getFrom().getFirstName(); // 发送者的名字
            TelegramMessageAdapter telegramMessageAdapter = new TelegramMessageAdapter(chatId.toString(), userInput, ragService, skills,this);
            ThreadUtils.instance().getExecutor().execute(telegramMessageAdapter);
        }

    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        super.onUpdatesReceived(updates);
    }


    @Override
    public void onRegister() {
        super.onRegister();
    }
}