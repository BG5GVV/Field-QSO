package com.ham.qso.domain.model

data class QCodeItem(
    val code: String,
    val question: String,
    val answer: String,
    val category: String = "Q简语"
)

object QCodeData {
    val qCodes: List<QCodeItem> = listOf(
        QCodeItem("QTH", "你的地理位置是哪里？", "我的地理位置是...", "常用"),
        QCodeItem("QSL", "你能确认收到/通联吗？", "我确认收到/确认通联卡片 (QSL Card)", "常用"),
        QCodeItem("QSO", "你能与某台直接通联吗？", "我可以与某台直接通联 / 通联会话", "常用"),
        QCodeItem("QRZ", "谁在呼叫我？", "正在呼叫您的是...", "常用"),
        QCodeItem("QSY", "我需要改变工作频率吗？", "请改到频率...发射/接收", "常用"),
        QCodeItem("QRM", "你受到其他电台的人为干扰吗？", "我正受到他台人为干扰", "信号与干扰"),
        QCodeItem("QRN", "你受到天电/自然杂音干扰吗？", "我受到天电(雷电/静电)干扰", "信号与干扰"),
        QCodeItem("QSB", "我的信号有衰落吗？", "你的信号强度正在衰落 (Fading)", "信号与干扰"),
        QCodeItem("QRO", "我需要增加发射功率吗？", "请增加发射功率 (大功率发射)", "功率与设备"),
        QCodeItem("QRP", "我需要减小发射功率吗？", "请减小发射功率 (小功率发射 ≤5W)", "功率与设备"),
        QCodeItem("QRT", "我要停止拍发/关机吗？", "停止拍发 / 正在关机退出通联", "状态"),
        QCodeItem("QRV", "你准备好了吗？", "我已经准备好了，可以通联", "状态"),
        QCodeItem("QRX", "你什么时候会再呼叫我？", "请稍候，我将在...分钟后再呼叫", "状态"),
        QCodeItem("QRL", "你忙吗？此频率有人占用吗？", "我正忙 / 本频率正在使用中 (Is this frequency in use?)", "频率使用"),
        QCodeItem("QRG", "你能告诉我我的确切频率吗？", "你的确切频率是...", "频率使用"),
        QCodeItem("QTR", "现在的准确时间是？", "现在的确切时间是... (UTC)", "常用"),
        QCodeItem("73", "美好祝愿 (Best Regards)", "致以美好的祝愿与问候！", "业余无线电礼仪"),
        QCodeItem("88", "爱与吻 (Love and Kisses)", "女士/YL 之间友好的问候致意", "业余无线电礼仪"),
        QCodeItem("CQ", "普遍呼叫 (Calling Any Station)", "普遍呼叫所有能够收听到的电台", "呼叫"),
        QCodeItem("DE", "来自... (From / This is)", "由...电台发出呼叫 (例如: CQ DE BG7xxx)", "呼叫"),
        QCodeItem("K", "请回答 / 发送完毕 (Over / Go ahead)", "邀请任何电台发射回答", "CW 缩语"),
        QCodeItem("KN", "指定呼叫某台回答 (Go ahead, named station only)", "仅邀请被指定的电台回答", "CW 缩语"),
        QCodeItem("SK", "通联彻底结束 (End of contact / Silent Key)", "本次通联全部结束 / 纪念故去的无线电爱好者", "CW 缩语"),
        QCodeItem("RST", "信号报告 (Readability, Strength, Tone)", "可辨度(1-5)、信号强度(1-9)、音调(1-9/FT8 dB)", "信号报告")
    )

    fun search(query: String): List<QCodeItem> {
        if (query.isBlank()) return qCodes
        val q = query.trim().lowercase()
        return qCodes.filter {
            it.code.lowercase().contains(q) ||
            it.question.lowercase().contains(q) ||
            it.answer.lowercase().contains(q) ||
            it.category.lowercase().contains(q)
        }
    }
}
