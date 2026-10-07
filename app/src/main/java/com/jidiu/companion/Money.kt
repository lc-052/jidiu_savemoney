package com.jidiu.companion

import java.math.BigDecimal
import java.math.RoundingMode

const val MAX_ENTRY_CENTS = 99_999_999_900L
enum class EntryKind { EXPENSE, INCOME, SAVING, WITHDRAWAL }
data class MoneyEntry(val id: Long, val kind: EntryKind, val cents: Long, val category: String, val note: String, val createdAt: Long)
enum class ItemCategory(val label: String) { CLOTHES("衣服"), HAIR("发型"), HAIR_COLOR("发色"), ACCESSORY("装饰"), BACKGROUND("背景"), FURNITURE("家具") }
data class VillageItem(val id: String, val name: String, val subtitle: String, val priceCents: Long, val category: ItemCategory, val requiredHairId: String? = null) {
    val slot: String get() = when(category) { ItemCategory.FURNITURE -> id; ItemCategory.HAIR_COLOR -> "HAIR_COLOR_$requiredHairId"; else -> category.name }
}
object ItemCatalog {
    val items = listOf(
        VillageItem("hoodie", "蜜桃连帽衫", "软软的日常，温暖的小连帽衫", 5000, ItemCategory.CLOTHES),
        VillageItem("sailor", "蓝白水手服", "带着海风的学院装", 8000, ItemCategory.CLOTHES),
        VillageItem("dress", "莓果小裙子", "荷叶裙摆和一颗小爱心", 10000, ItemCategory.CLOTHES),
        VillageItem("cardigan", "奶油针织套装", "软软开衫与格纹小裙子", 6000, ItemCategory.CLOTHES),
        VillageItem("overalls", "牛仔背带裤", "带着小口袋去散步", 7000, ItemCategory.CLOTHES),
        VillageItem("mintdress", "薄荷连帽裙", "清清爽爽的日常穿搭", 8000, ItemCategory.CLOTHES),
        VillageItem("yukata", "樱花浴衣", "粉色花瓣与温柔的腰结", 12000, ItemCategory.CLOTHES),
        VillageItem("bob", "齐刘海短发", "基础黑色，可另存 5 元选择发色", 3000, ItemCategory.HAIR),
        VillageItem("pinkhair", "双马尾", "基础黑色，可另存 5 元选择发色", 6000, ItemCategory.HAIR),
        VillageItem("silverhair", "披肩长发", "基础黑色，可另存 5 元选择发色", 8000, ItemCategory.HAIR),
        VillageItem("bun", "丸子头", "两个蓬松小丸子，基础黑色", 4000, ItemCategory.HAIR),
        VillageItem("ponytail", "高马尾", "轻快地晃一晃，基础黑色", 5000, ItemCategory.HAIR),
        VillageItem("braid", "双麻花辫", "一节节编起小心情，基础黑色", 6000, ItemCategory.HAIR),
        VillageItem("wave", "波浪长发", "像小小波浪垂下来，基础黑色", 8000, ItemCategory.HAIR),
        VillageItem("bow", "草莓蝴蝶结", "今天也要可可爱爱", 2000, ItemCategory.ACCESSORY),
        VillageItem("headphones", "星蓝耳机", "一起听一首喜欢的歌", 5000, ItemCategory.ACCESSORY),
        VillageItem("crown", "小星星发冠", "给认真生活的你加冕", 7000, ItemCategory.ACCESSORY),
        VillageItem("sakura", "樱花晴空", "粉色天空与飘落的花瓣", 5000, ItemCategory.BACKGROUND),
        VillageItem("forest", "森林晨光", "像素树影里的绿色早晨", 5000, ItemCategory.BACKGROUND),
        VillageItem("night", "星月之夜", "月亮与繁星陪你入眠", 8000, ItemCategory.BACKGROUND),
        VillageItem("plant", "小绿植", "让空空的房间长出新芽", 1000, ItemCategory.FURNITURE),
        VillageItem("book", "故事书架", "把喜欢的故事收进小家", 3000, ItemCategory.FURNITURE),
        VillageItem("lamp", "暖光落地灯", "一盏属于你的夜灯", 5000, ItemCategory.FURNITURE),
        VillageItem("rug", "云朵地毯", "踩在软软的云朵上", 5000, ItemCategory.FURNITURE),
        VillageItem("star", "星星挂饰", "把星星挂在房间里", 3000, ItemCategory.FURNITURE),
        VillageItem("bed", "草莓小床", "愿每个夜晚都有好梦", 15000, ItemCategory.FURNITURE),
        VillageItem("desk", "小木书桌", "留一个写下心事的角落", 8000, ItemCategory.FURNITURE),
        VillageItem("window", "晴空小窗", "把阳光请进来", 5000, ItemCategory.FURNITURE),
    ) + listOf("bob" to "短发", "pinkhair" to "双马尾", "silverhair" to "长发", "bun" to "丸子头", "ponytail" to "高马尾", "braid" to "麻花辫", "wave" to "波浪长发").map { (hairId, label) ->
        VillageItem("${hairId}_palette", "$label · 自由选色", "先拥有这个发型，再新存 5 元永久解锁选色，之后自由切换", 500, ItemCategory.HAIR_COLOR, hairId)
    }
}
data class Gift(val id: String, val name: String, val subtitle: String, val priceCents: Long)
object GiftCatalog {
    val gifts = listOf(
        Gift("heartbox", "存钱礼盒", "新存 10 元，给这次坚持留个纪念", 1000),
        Gift("flower", "鼓励小花", "新存 5 元，小小积累也值得庆祝", 500),
        Gift("cake", "庆祝蛋糕", "新存 20 元，庆祝离目标更近一步", 2000),
        Gift("bookgift", "成长手账", "新存 30 元，记下这次存钱的进步", 3000),
        Gift("starjar", "星星存钱罐", "新存 50 元，把小目标一点点装满", 5000),
        Gift("trophy", "坚持奖杯", "新存 100 元，奖励认真储蓄的自己", 10000),
    )
}
data class CompanionSnapshot(
    val entries: List<MoneyEntry>, val ownedItemIds: Set<String>, val equippedItems: Map<String, String>,
    val savingsCents: Long, val goalCents: Long, val companionName: String, val giftCount: Int,
    val customGifts: List<Gift> = emptyList(), val looks: List<SavedLook> = emptyList(),
) {
    fun isEquipped(item: VillageItem): Boolean = if(item.category==ItemCategory.HAIR_COLOR) equippedItems[item.slot]!=null && equippedItems[ItemCategory.HAIR.name]==item.requiredHairId else equippedItems[item.slot] == item.id
}
data class SavedLook(val id: Long,val name: String,val equipment: Map<String,String>)

// Every acquisition is backed by its own new saving entry, never by an old balance.
fun validateItemDeposit(item: VillageItem, ownedIds: Set<String>, cents: Long) {
    require(item.id !in ownedIds) { "这件物品已经永久拥有，可以直接使用。" }
    require(item.requiredHairId == null || item.requiredHairId in ownedIds) { "请先获得对应发型，再选择发色。" }
    require(cents == item.priceCents) { "本次存款须为 ¥${formatMoney(item.priceCents)}。" }
}
fun validateGiftDeposit(gift: Gift, cents: Long) {
    require(cents == gift.priceCents) { "本次赠送须新存 ¥${formatMoney(gift.priceCents)}。" }
}
fun parseMoney(text: String): Long? {
    val value = text.trim()
    if (!Regex("[0-9]{1,9}(\\.[0-9]{1,2})?").matches(value)) return null
    return runCatching { BigDecimal(value).movePointRight(2).longValueExact() }.getOrNull()?.takeIf { it in 1..MAX_ENTRY_CENTS }
}
fun formatMoney(cents: Long): String = BigDecimal.valueOf(cents, 2).setScale(2, RoundingMode.UNNECESSARY).toPlainString()
fun savingsBalance(entries: List<MoneyEntry>): Long = entries.fold(0L) { sum, entry ->
    when (entry.kind) {
        EntryKind.SAVING -> Math.addExact(sum, entry.cents)
        EntryKind.WITHDRAWAL -> Math.subtractExact(sum, entry.cents)
        else -> sum
    }
}
fun validateSavingsHistory(entries: List<MoneyEntry>) {
    var balance = 0L
    entries.sortedWith(compareBy<MoneyEntry> { it.createdAt }.thenBy { it.id }).forEach {
        balance = Math.addExact(balance, when (it.kind) { EntryKind.SAVING -> it.cents; EntryKind.WITHDRAWAL -> -it.cents; else -> 0L })
        require(balance >= 0) { "这笔操作会让储蓄余额不足，请先调整相关的取出记录。" }
    }
}
