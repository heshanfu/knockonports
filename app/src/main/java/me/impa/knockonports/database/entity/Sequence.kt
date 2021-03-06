/*
 * Copyright (c) 2018 Alexander Yaburov
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package me.impa.knockonports.database.entity

import android.arch.persistence.room.*
import me.impa.knockonports.data.KnockType
import me.impa.knockonports.json.IcmpData
import me.impa.knockonports.json.PortData

@Entity(tableName = "tbSequence")
data class Sequence(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "_id")
        var id: Long?,
        @ColumnInfo(name = "_name")
        var name: String?,
        @ColumnInfo(name = "_host")
        var host: String?,
        @ColumnInfo(name = "_timeout")
        var timeout: Int?,
        @ColumnInfo(name = "_order")
        var order: Int?,
        @ColumnInfo(name = "_delay")
        var delay: Int?,
        @ColumnInfo(name = "_udp_content")
        var udpContent: String?,
        @ColumnInfo(name = "_application")
        var application: String?,
        @ColumnInfo(name = "_base64")
        var base64: Int?,
        @ColumnInfo(name = "_port_string")
        var ports: List<PortData>?,
        @ColumnInfo(name = "_application_name")
        var applicationName: String?,
        @ColumnInfo(name = "_type")
        var type: KnockType?,
        @ColumnInfo(name = "_icmp_string")
        var icmp: List<IcmpData>?
) {

    fun getReadableDescription(): String? = when (type) {
        KnockType.PORT -> getReadablePortString()
        KnockType.ICMP -> getReadableIcmpString()
        else -> null
    }


    private fun getReadablePortString(): String? =
            ports?.filter { it.value != null }?.joinToString(", ") { it.value.toString() + ":" + it.type.name }

    private fun getReadableIcmpString(): String? =
            icmp?.filter { it.size != null }?.joinToString(", ") { it.size.toString() + "x" + Math.max(1, it.count ?: 0).toString()  }

    companion object {
        const val INVALID_SEQ_ID = -100500L

    }
}