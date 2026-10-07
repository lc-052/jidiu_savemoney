package com.jidiu.companion

import org.junit.Assert.*
import org.junit.Test

class MoneyTest {
    private fun entry(id:Long,kind:EntryKind,cents:Long)=MoneyEntry(id,kind,cents,"测试","",id)
    @Test fun decimalAmountsAreExactAndInvalidInputIsRejected() {
        assertEquals(10L,parseMoney("0.10"));assertEquals(1L,parseMoney("0.01"));assertEquals(1899L,parseMoney("18.99"))
        assertEquals("0.30",formatMoney(parseMoney("0.1")!!+parseMoney("0.2")!!))
        listOf("","0","-2","1.234","1e3","1,000","NaN","9999999999").forEach {assertNull(it,parseMoney(it))}
    }
    @Test fun itemAndRepeatedGiftDepositsIncreaseSavingsWithoutCountingAsExpense() {
        val item=ItemCatalog.items.first {it.id=="hoodie"};val gift=GiftCatalog.gifts.first()
        validateItemDeposit(item,emptySet(),5000);validateGiftDeposit(gift,1000);validateGiftDeposit(gift,1000)
        val records=listOf(entry(1,EntryKind.INCOME,50000),entry(2,EntryKind.EXPENSE,1800),entry(3,EntryKind.SAVING,item.priceCents),entry(4,EntryKind.SAVING,gift.priceCents),entry(5,EntryKind.SAVING,gift.priceCents))
        assertEquals(7000L,savingsBalance(records));assertEquals(1800L,records.filter {it.kind==EntryKind.EXPENSE}.sumOf {it.cents})
    }
    @Test(expected=IllegalArgumentException::class) fun oldOwnershipCannotBeAcquiredAgain() {
        val item=ItemCatalog.items.first();validateItemDeposit(item,setOf(item.id),item.priceCents)
    }
    @Test(expected=IllegalArgumentException::class) fun cheaperDepositCannotAcquireFiftyYuanItem() {
        validateItemDeposit(ItemCatalog.items.first {it.id=="hoodie"},emptySet(),1000)
    }
    @Test(expected=IllegalArgumentException::class) fun giftRequiresItsOwnExactSingleDeposit() {
        validateGiftDeposit(GiftCatalog.gifts.first(),1)
    }
    @Test fun clothingSharesOneSlotAndFurnitureCanCoexist() {
        val clothes=ItemCatalog.items.filter {it.category==ItemCategory.CLOTHES}
        assertEquals(1,clothes.map {it.slot}.toSet().size)
        val furniture=ItemCatalog.items.filter {it.category==ItemCategory.FURNITURE}
        assertEquals(furniture.size,furniture.map {it.slot}.toSet().size)
        assertEquals(ItemCatalog.items.size,ItemCatalog.items.map {it.id}.toSet().size)
        assertTrue(ItemCategory.entries.all {category -> ItemCatalog.items.any {it.category==category}})
    }
    @Test(expected=IllegalArgumentException::class) fun removingDepositCannotOverdrawWithdrawal() {
        validateSavingsHistory(listOf(entry(2,EntryKind.WITHDRAWAL,1000),entry(3,EntryKind.SAVING,5000)))
    }
    @Test fun withdrawalsOnlyChangeSavingsBalance() {
        val records=listOf(entry(1,EntryKind.SAVING,5000),entry(2,EntryKind.WITHDRAWAL,2000))
        validateSavingsHistory(records);assertEquals(3000L,savingsBalance(records))
    }
    @Test fun hairColorUnlockCostsFiveYuanAndRequiresItsOwnHairstyle() {
        val palette=ItemCatalog.items.first {it.id=="bob_palette"}
        assertEquals(500L,palette.priceCents)
        assertTrue(runCatching {validateItemDeposit(palette,emptySet(),500)}.isFailure)
        assertTrue(runCatching {validateItemDeposit(palette,setOf("pinkhair"),500)}.isFailure)
        assertTrue(runCatching {validateItemDeposit(palette,setOf("bob"),1)}.isFailure)
        validateItemDeposit(palette,setOf("bob"),500)
        assertTrue(runCatching {validateItemDeposit(palette,setOf("bob","bob_palette"),500)}.isFailure)
        assertNotEquals(palette.slot,ItemCatalog.items.first {it.id=="pinkhair_palette"}.slot)
    }
    @Test fun retiredSkinItemsCannotBeSelected() {
        assertFalse(ItemCatalog.items.any {it.id.startsWith("skin_")})
        assertFalse(ItemCategory.entries.any {it.label=="肤色"})
    }
}
