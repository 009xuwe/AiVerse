package com.aiverse.feature.agent.domain.command

import com.aiverse.R
import javax.inject.Inject

/**
 * /help —— 触发一次 agent 回合，用 `webfetch` 检索 AiVerse 官网文档并基于结果作答（附来源链接）。
 *
 * 检索入口为官网搜索接口，返回 JSON `[{url,title,snippet}]`；结果不理想时智能改写关键词重搜。
 * 指令正文内联在 [PROMPT]，便于随检索接口一并调整，无需改动 `prompts/` 资产。
 */
class HelpCommandHandler @Inject constructor() : ActionCommandHandler {
    override val name = "help"
    override val descriptionRes = R.string.slash_command_help_desc
    override val acceptsArgs = true

    override fun execute(context: SlashCommandContext, args: String) {
        context.showHelp(PROMPT.replace("{question}", args.trim()))
    }

    companion object {
        /** 指令正文：{question} 占位符替换为 `/help` 之后的用户输入。 */
        const val PROMPT = """用户希望通过 `/help` 了解 AiVerse 的功能或用法。请检索 AiVerse 官网文档后作答。

用户的问题：
{question}
（若上面为空，则介绍 AiVerse 的核心能力与常见用法入口。）

检索步骤：
1. 从问题中提取 1~4 个检索关键词（去掉「怎么」「如何」「是什么」等停用词）。
2. 用 `webfetch` 抓取官网搜索接口：https://www.aivapp.cn/api/search.php?wd=关键词
   （多个关键词用空格或 `+` 连接；接口返回 JSON 数组，每项含 url / title / snippet。）
3. 若结果不理想，智能改写关键词重搜：换同义词、拆分或合并词、中英文互换。例如搜不到「插件」就依次试「插件 开发」「plugin」「plugin sdk」；搜不到「思考强度」就试「推理」「reasoning」。最多尝试 3~5 次。
4. 从命中的结果里挑最相关的 1~3 篇，必要时再 `webfetch` 抓取正文页面确认细节。
5. 基于检索到的官方文档组织回答：先给结论与操作步骤，再附「来源」小节，用 Markdown 链接列出引用的文档 URL 与标题。

官网为 AiVerse 文档站：中文主站 https://www.aivapp.cn/ ，英文文档在 /en/ 路径（如 https://www.aivapp.cn/en/ ）。回答语言跟随用户提问语言。
若多次检索仍无有效结果，或 `webfetch` 不可用/网络失败，则基于已有知识尽力作答，并明确说明「未能联网核实，以下内容可能不是最新」。"""
    }
}
