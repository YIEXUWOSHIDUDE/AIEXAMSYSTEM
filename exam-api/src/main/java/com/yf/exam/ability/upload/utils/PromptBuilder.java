package com.yf.exam.ability.upload.utils;

import com.yf.exam.config.PromptConfig;

public class PromptBuilder {

    /**
     * 构建用于大模型解析试题的 prompt
     * @param paperText 试卷原文内容
     * @return 拼接后的 prompt
     */
    public static String buildExtractQuestionPrompt(String paperText) {
        return PromptConfig.EXTRACT_QUESTION_PROMPT + "\n" + paperText;
    }
}
