package com.jidiu.companion

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CompanionStore(context: Context, databaseName: String = "companion.db") : SQLiteOpenHelper(context, databaseName, null, 6) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE entries (id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, cents INTEGER NOT NULL CHECK(cents > 0), category TEXT NOT NULL, note TEXT NOT NULL, created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE settings (id INTEGER PRIMARY KEY CHECK(id = 1), name TEXT NOT NULL, goal_cents INTEGER NOT NULL CHECK(goal_cents > 0))")
        db.execSQL("INSERT INTO settings (id, name, goal_cents) VALUES (1, '寄丢', 50000)")
        createVillageTables(db)
        createExtras(db)
    }
    private fun createVillageTables(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE owned_items (id TEXT PRIMARY KEY, acquired_at INTEGER NOT NULL, entry_id INTEGER)")
        db.execSQL("CREATE TABLE equipment (slot TEXT PRIMARY KEY, item_id TEXT NOT NULL)")
        db.execSQL("CREATE TABLE gift_events (id INTEGER PRIMARY KEY AUTOINCREMENT, gift_id TEXT NOT NULL, cents INTEGER NOT NULL, entry_id INTEGER NOT NULL UNIQUE, created_at INTEGER NOT NULL)")
    }
    private fun createExtras(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS custom_gifts (id TEXT PRIMARY KEY, name TEXT NOT NULL, cents INTEGER NOT NULL CHECK(cents > 0))")
        db.execSQL("CREATE TABLE IF NOT EXISTS saved_looks (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, equipment TEXT NOT NULL)")
    }
    private fun readCustomGifts(db: SQLiteDatabase): List<Gift> = buildList {
        db.rawQuery("SELECT id,name,cents FROM custom_gifts ORDER BY rowid",null).use {c ->
            while(c.moveToNext()) add(Gift(c.getString(0),c.getString(1),"你定下的小奖励，每次赠送都对应一笔新存款",c.getLong(2)))
        }
    }
    private fun readLooks(db: SQLiteDatabase): List<SavedLook> = buildList {
        db.rawQuery("SELECT id,name,equipment FROM saved_looks ORDER BY id DESC",null).use {c ->
            while(c.moveToNext()) {
                val json=org.json.JSONObject(c.getString(2))
                add(SavedLook(c.getLong(0),c.getString(1),json.keys().asSequence().associateWith {json.getString(it)}))
            }
        }
    }
    fun addCustomGift(name: String,cents: Long) {
        require(name.trim().length in 1..20) {"礼物名称需要 1 到 20 个字。"}
        require(cents in 1..MAX_ENTRY_CENTS) {"请输入有效的单次存款金额。"}
        transaction {db ->
            require(readCustomGifts(db).size<30) {"最多保存 30 种自定义礼物。"}
            db.insertOrThrow("custom_gifts",null,ContentValues().apply {
                put("id","custom_${java.util.UUID.randomUUID()}");put("name",name.trim());put("cents",cents)
            })
        }
    }
    private fun validateEquipment(equipment: Map<String,String>,ids: Set<String>) {
        for((slot,id) in equipment) {
            if(slot.startsWith("HAIR_COLOR_")) {
                val hairId=slot.removePrefix("HAIR_COLOR_")
                require(hairId in ids && "${hairId}_palette" in ids)
                require(id in listOf("brown","pink","silver").map {"${hairId}_$it"})
            } else require(ItemCatalog.items.any {it.id==id && it.slot==slot} && id in ids)
        }
    }
    fun saveLook(name: String) {
        require(name.trim().length in 1..16) {"穿搭名称需要 1 到 16 个字。"}
        transaction {db ->
            val state=snapshot()
            require(state.looks.size<20) {"最多保存 20 套穿搭。"}
            val clothes=state.equippedItems.filterKeys {it=="HAIR" || it=="CLOTHES" || it=="ACCESSORY" || it=="HAIR_COLOR_${state.equippedItems["HAIR"]}"}
            db.insertOrThrow("saved_looks",null,ContentValues().apply {put("name",name.trim());put("equipment",org.json.JSONObject(clothes).toString())})
        }
    }
    fun applyLook(id: Long) {
        transaction {db ->
            val look=readLooks(db).firstOrNull {it.id==id} ?: error("找不到这套穿搭。")
            validateEquipment(look.equipment,owned(db))
            db.delete("equipment","slot IN ('HAIR','CLOTHES','ACCESSORY')",null)
            look.equipment["HAIR"]?.let {db.delete("equipment","slot = ?",arrayOf("HAIR_COLOR_$it"))}
            for((slot,itemId) in look.equipment) db.insertOrThrow("equipment",null,ContentValues().apply {put("slot",slot);put("item_id",itemId)})
        }
    }
    private val backupTables=linkedMapOf(
        "entries" to listOf("id","kind","cents","category","note","created_at"),
        "settings" to listOf("id","name","goal_cents"),
        "owned_items" to listOf("id","acquired_at","entry_id"),
        "equipment" to listOf("slot","item_id"),
        "gift_events" to listOf("id","gift_id","cents","entry_id","created_at"),
        "custom_gifts" to listOf("id","name","cents"),
        "saved_looks" to listOf("id","name","equipment"),
    )
    fun exportBackup(): String {
        val root=org.json.JSONObject().put("app","jidiu-companion").put("format",1)
        transaction {db ->
            val tables=org.json.JSONObject()
            for((table,columns) in backupTables) {
                val rows=org.json.JSONArray()
                db.rawQuery("SELECT ${columns.joinToString(",")} FROM $table",null).use {c ->
                    while(c.moveToNext()) {
                        val row=org.json.JSONObject()
                        columns.forEachIndexed {i,column -> row.put(column,when(c.getType(i)) {
                            android.database.Cursor.FIELD_TYPE_NULL -> org.json.JSONObject.NULL
                            android.database.Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                            else -> c.getString(i)
                        })}
                        rows.put(row)
                    }
                }
                tables.put(table,rows)
            }
            root.put("tables",tables)
        }
        return root.toString()
    }
    fun backupSummary(text: String): String {
        val root=parseBackup(text)
        val tables=root.getJSONObject("tables")
        return "账本 ${tables.getJSONArray("entries").length()} 条 · 物品 ${tables.getJSONArray("owned_items").length()} 件 · 自定义礼物 ${tables.getJSONArray("custom_gifts").length()} 种\n恢复会替换这台手机当前的数据。"
    }
    private fun parseBackup(text: String): org.json.JSONObject {
        require(text.toByteArray(Charsets.UTF_8).size<=5*1024*1024) {"备份文件过大。"}
        val root=org.json.JSONObject(text)
        require(root.getString("app")=="jidiu-companion" && root.getInt("format")==1) {"不是支持的寄丢暂存备份。"}
        val tables=root.getJSONObject("tables")
        require(tables.keys().asSequence().toSet()==backupTables.keys) {"备份内容不完整。"}
        require(tables.getJSONArray("settings").length()==1)
        for((table,columns) in backupTables) {
            val rows=tables.getJSONArray(table);require(rows.length()<=50000)
            for(i in 0 until rows.length()) require(rows.getJSONObject(i).keys().asSequence().toSet()==columns.toSet())
        }
        return root
    }
    fun restoreBackup(text: String) {
        val tables=parseBackup(text).getJSONObject("tables")
        transaction {db ->
            for(table in backupTables.keys) db.delete(table,null,null)
            for((table,columns) in backupTables) {
                val rows=tables.getJSONArray(table)
                for(i in 0 until rows.length()) {
                    val row=rows.getJSONObject(i)
                    val values=ContentValues()
                    for(column in columns) {
                        val v=row.get(column)
                        when(v) {
                            org.json.JSONObject.NULL -> values.putNull(column)
                            is Number -> {require(v is Int || v is Long);values.put(column,v.toLong())}
                            is String -> {require(v.length<=100000);values.put(column,v)}
                            else -> error("备份含有无效数据。")
                        }
                    }
                    db.insertOrThrow(table,null,values)
                }
            }
            val state=snapshot()
            require(state.companionName.length in 1..12 && state.goalCents in 1..MAX_ENTRY_CENTS)
            state.entries.forEach {require(it.cents in 1..MAX_ENTRY_CENTS && it.category.length in 1..30 && it.note.length<=160 && it.createdAt>=0)}
            validateSavingsHistory(state.entries)
            require(state.ownedItemIds.all {id -> ItemCatalog.items.any {it.id==id}})
            validateEquipment(state.equippedItems,state.ownedItemIds)
            require(state.customGifts.size<=30 && state.customGifts.all {it.id.startsWith("custom_") && it.name.length in 1..20 && it.priceCents in 1..MAX_ENTRY_CENTS})
            require(state.looks.size<=20)
            state.looks.forEach {require(it.name.length in 1..16);validateEquipment(it.equipment,state.ownedItemIds)}
            db.rawQuery("SELECT cents FROM gift_events",null).use {c -> while(c.moveToNext()) require(c.getLong(0) in 1..MAX_ENTRY_CENTS)}
        }
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createVillageTables(db)
            // Preserve previously earned furniture without fabricating new saving records.
            db.rawQuery("SELECT id, delivered_at FROM gifts", null).use { cursor ->
                while (cursor.moveToNext()) {
                    val item = ItemCatalog.items.find { it.id == cursor.getString(0) } ?: continue
                    db.insertOrThrow("owned_items", null, ContentValues().apply { put("id", item.id); put("acquired_at", cursor.getLong(1)) })
                    equip(db, item)
                }
            }
        }
        if (oldVersion < 3) {
            db.execSQL("UPDATE settings SET name = '寄丢' WHERE id = 1 AND name = '小满'")
        }
        if(oldVersion<4) {
            // Earlier hairstyles included a fixed color; keep that acquired appearance.
            for((hairId,color) in listOf("bob" to "brown","pinkhair" to "pink","silverhair" to "silver")) {
                if(hairId !in owned(db)) continue
                db.insertWithOnConflict("owned_items",null,ContentValues().apply {put("id","${hairId}_palette");put("acquired_at",System.currentTimeMillis())},SQLiteDatabase.CONFLICT_IGNORE)
                db.insertWithOnConflict("equipment",null,ContentValues().apply {put("slot","HAIR_COLOR_$hairId");put("item_id","${hairId}_$color")},SQLiteDatabase.CONFLICT_IGNORE)
            }
        }
        if(oldVersion<5) {
            // Retire appearance options while keeping every real deposit and its history.
            db.delete("equipment","slot = ?",arrayOf("SKIN"))
            for(id in listOf("skin_ivory","skin_peach","skin_wheat")) db.delete("owned_items","id = ?",arrayOf(id))
        }
        if(oldVersion<6) createExtras(db)
    }
    private fun readEntries(db: SQLiteDatabase): List<MoneyEntry> = buildList {
        db.rawQuery("SELECT id, kind, cents, category, note, created_at FROM entries ORDER BY created_at DESC, id DESC", null).use { cursor ->
            while (cursor.moveToNext()) add(MoneyEntry(cursor.getLong(0), EntryKind.valueOf(cursor.getString(1)), cursor.getLong(2), cursor.getString(3), cursor.getString(4), cursor.getLong(5)))
        }
    }
    private fun owned(db: SQLiteDatabase): Set<String> = buildSet {
        db.rawQuery("SELECT id FROM owned_items", null).use { cursor -> while (cursor.moveToNext()) add(cursor.getString(0)) }
    }
    fun snapshot(): CompanionSnapshot {
        val db = readableDatabase
        val entries = readEntries(db)
        val equipment = buildMap {
            db.rawQuery("SELECT slot, item_id FROM equipment", null).use { cursor -> while (cursor.moveToNext()) put(cursor.getString(0), cursor.getString(1)) }
        }
        val giftCount = db.rawQuery("SELECT COUNT(*) FROM gift_events", null).use { it.moveToFirst(); it.getInt(0) }
        db.rawQuery("SELECT name, goal_cents FROM settings WHERE id = 1", null).use { cursor ->
            check(cursor.moveToFirst()) { "无法读取设置，请保留应用数据。" }
            return CompanionSnapshot(entries, owned(db), equipment, savingsBalance(entries), cursor.getLong(1), cursor.getString(0), giftCount,readCustomGifts(db),readLooks(db))
        }
    }
    private fun transaction(block: (SQLiteDatabase) -> Unit) {
        val db = writableDatabase
        db.beginTransaction()
        try { block(db); db.setTransactionSuccessful() } finally { db.endTransaction() }
    }
    private fun insertEntry(db: SQLiteDatabase, kind: EntryKind, cents: Long, category: String, note: String): Long {
        require(cents in 1..MAX_ENTRY_CENTS) { "请输入有效金额，最多保留两位小数。" }
        require(category.isNotBlank() && category.length <= 30) { "请选择记账分类。" }
        require(note.length <= 160) { "备注最多 160 字。" }
        val latestTime = db.rawQuery("SELECT MAX(created_at) FROM entries", null).use { it.moveToFirst(); it.getLong(0) }
        return db.insertOrThrow("entries", null, ContentValues().apply {
            put("kind", kind.name); put("cents", cents); put("category", category.trim()); put("note", note.trim())
            put("created_at", maxOf(System.currentTimeMillis(), latestTime))
        })
    }
    fun addEntry(kind: EntryKind, cents: Long, category: String, note: String) {
        transaction { db ->
            if (kind == EntryKind.WITHDRAWAL) require(cents <= savingsBalance(readEntries(db))) { "取出金额不能超过已存余额。" }
            insertEntry(db, kind, cents, category, note)
        }
    }
    fun deleteEntry(id: Long) {
        transaction { db ->
            val entries = readEntries(db)
            require(entries.any { it.id == id }) { "这条记录已不存在。" }
            validateSavingsHistory(entries.filterNot { it.id == id })
            check(db.delete("entries", "id = ?", arrayOf(id.toString())) == 1)
        }
    }
    private fun equip(db: SQLiteDatabase, item: VillageItem) {
        item.requiredHairId?.let { hairId -> equip(db, ItemCatalog.items.first { it.id == hairId }) }
        val selected=if(item.category==ItemCategory.HAIR_COLOR) db.rawQuery("SELECT item_id FROM equipment WHERE slot = ?",arrayOf(item.slot)).use {if(it.moveToFirst()) it.getString(0) else "${item.requiredHairId}_brown"} else item.id
        db.insertWithOnConflict("equipment", null, ContentValues().apply { put("slot", item.slot); put("item_id", selected) }, SQLiteDatabase.CONFLICT_REPLACE).also { check(it != -1L) }
    }
    fun saveForItem(id: String, confirmedCents: Long) {
        val item = ItemCatalog.items.find { it.id == id } ?: error("找不到这件物品。")
        transaction { db ->
            validateItemDeposit(item, owned(db), confirmedCents)
            val entryId = insertEntry(db, EntryKind.SAVING, confirmedCents, "物品存款", "永久获得：${item.name}")
            db.insertOrThrow("owned_items", null, ContentValues().apply { put("id", id); put("acquired_at", System.currentTimeMillis()); put("entry_id", entryId) })
            equip(db, item)
        }
    }
    fun toggleItem(id: String) {
        val item = ItemCatalog.items.find { it.id == id } ?: error("找不到这件物品。")
        transaction { db ->
            require(id in owned(db)) { "请先为这件物品存一笔钱。" }
            val matchingHair=item.category!=ItemCategory.HAIR_COLOR || db.rawQuery("SELECT item_id FROM equipment WHERE slot = ?",arrayOf(ItemCategory.HAIR.name)).use {it.moveToFirst() && it.getString(0)==item.requiredHairId}
            val active = matchingHair && db.rawQuery("SELECT item_id FROM equipment WHERE slot = ?", arrayOf(item.slot)).use { it.moveToFirst() && (item.category==ItemCategory.HAIR_COLOR || it.getString(0) == id) }
            if (active) db.delete("equipment", "slot = ?", arrayOf(item.slot)) else equip(db, item)
        }
    }
    fun selectHairColor(hairId: String, color: String) {
        require(color in setOf("black", "brown", "pink", "silver")) { "请选择提供的发色。" }
        val hair=ItemCatalog.items.firstOrNull {it.id==hairId && it.category==ItemCategory.HAIR} ?: error("找不到这个发型。")
        transaction {db ->
            val ids=owned(db)
            require(hairId in ids) { "请先获得这个发型。" }
            require(color=="black" || "${hairId}_palette" in ids) { "请先新存 5 元解锁这个发型的选色功能。" }
            equip(db,hair)
            if(color=="black") db.delete("equipment","slot = ?",arrayOf("HAIR_COLOR_$hairId"))
            else db.insertWithOnConflict("equipment",null,ContentValues().apply {put("slot","HAIR_COLOR_$hairId");put("item_id","${hairId}_$color")},SQLiteDatabase.CONFLICT_REPLACE).also {check(it != -1L)}
        }
    }
    fun saveForGift(id: String, confirmedCents: Long) {
        val gift = (GiftCatalog.gifts+readCustomGifts(readableDatabase)).find { it.id == id } ?: error("找不到这份礼物。")
        transaction { db ->
            validateGiftDeposit(gift, confirmedCents)
            val entryId = insertEntry(db, EntryKind.SAVING, confirmedCents, "礼物存款", "赠送：${gift.name}")
            db.insertOrThrow("gift_events", null, ContentValues().apply {
                put("gift_id", id); put("cents", confirmedCents); put("entry_id", entryId); put("created_at", System.currentTimeMillis())
            })
        }
    }
    fun updateSettings(name: String, goalCents: Long) {
        require(name.trim().length in 1..12) { "名字需要 1 到 12 个字。" }
        require(goalCents in 1..MAX_ENTRY_CENTS) { "请输入有效的储蓄目标。" }
        val values = ContentValues().apply { put("name", name.trim()); put("goal_cents", goalCents) }
        check(writableDatabase.update("settings", values, "id = 1", null) == 1) { "设置未保存，请重试。" }
    }
}
