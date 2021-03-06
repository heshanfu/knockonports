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

package me.impa.knockonports.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import com.savvyapps.togglebuttonlayout.ToggleButtonLayout
import me.impa.knockonports.R
import me.impa.knockonports.data.ContentEncoding
import me.impa.knockonports.data.KnockType
import me.impa.knockonports.data.PortType
import me.impa.knockonports.ext.afterTextChanged
import me.impa.knockonports.json.IcmpData
import me.impa.knockonports.json.PortData
import me.impa.knockonports.viewadapter.IcmpAdapter
import me.impa.knockonports.viewadapter.KnockerItemTouchHelper
import me.impa.knockonports.viewadapter.PortAdapter
import me.impa.knockonports.viewmodel.MainViewModel
import org.jetbrains.anko.find

class BasicSettingsFragment : Fragment() {

    private val mainViewModel by lazy { ViewModelProviders.of(activity).get(MainViewModel::class.java) }
    private val portAdapter by lazy { PortAdapter() }
    private val icmpAdapter by lazy { IcmpAdapter(context) }
    private val nameEdit by lazy { view!!.findViewById<TextInputEditText>(R.id.edit_sequence_name) }
    private val hostEdit by lazy { view!!.findViewById<TextInputEditText>(R.id.edit_sequence_host) }
    private val addPortButton by lazy { view!!.findViewById<Button>(R.id.button_add_port) }
    private val addIcmpButton by lazy { view!!.findViewById<Button>(R.id.button_add_icmp) }
    private val scrollView by lazy { view!!.findViewById<ScrollView>(R.id.basic_settings_view) }
    private val recyclerPortView by lazy { view!!.findViewById<RecyclerView>(R.id.recycler_ports) }
    private val recyclerIcmpView by lazy { view!!.findViewById<RecyclerView>(R.id.recycler_icmp) }
    private val sequenceTypeGroup by lazy { view!!.findViewById<ToggleButtonLayout>(R.id.selector_seq_type) }
    private val icmpLayout by lazy { view!!.findViewById<LinearLayout>(R.id.type_icmp_layout) }
    private val portsLayout by lazy { view!!.findViewById<LinearLayout>(R.id.type_ports_layout) }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater!!.inflate(R.layout.fragment_sequence_config_basic, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel.getDirtySequence().observe(this, Observer {
            nameEdit.setText(it?.name)
            hostEdit.setText(it?.host)
            sequenceTypeGroup.setToggled(if (it?.type == KnockType.ICMP) {
                R.id.type_icmp
            } else {
                R.id.type_ports
            }, true)
            showSequence(it?.type ?: KnockType.PORT)
        })

        nameEdit.afterTextChanged {
            mainViewModel.getDirtySequence().value?.name = it
        }

        hostEdit.afterTextChanged {
            mainViewModel.getDirtySequence().value?.host = it
        }

        recyclerPortView.layoutManager = LinearLayoutManager(activity)
        recyclerPortView.adapter = portAdapter
        recyclerPortView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        val portTouchHelper = ItemTouchHelper(KnockerItemTouchHelper(portAdapter, ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT))
        portTouchHelper.attachToRecyclerView(recyclerPortView)
        portAdapter.onStartDrag = {
            portTouchHelper.startDrag(it)
        }

        mainViewModel.getDirtyPorts().observe(this, Observer {
            portAdapter.items = it ?: mutableListOf()
        })

        mainViewModel.getDirtyIcmp().observe(this, Observer {
            icmpAdapter.items = it ?: mutableListOf()
        })

        recyclerIcmpView.layoutManager = LinearLayoutManager(activity)
        recyclerIcmpView.adapter = icmpAdapter
        recyclerIcmpView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        val icmpTouchHelper = ItemTouchHelper(KnockerItemTouchHelper(icmpAdapter, ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT))
        icmpTouchHelper.attachToRecyclerView(recyclerIcmpView)
        icmpAdapter.onStartDrag = {
            icmpTouchHelper.startDrag(it)
        }

        addPortButton.setOnClickListener {
            val model = PortData(null, PortType.UDP)
            portAdapter.addItem(model)
            scrollView.post {
                scrollView.fullScroll(View.FOCUS_DOWN)
                recyclerPortView.getChildAt(portAdapter.itemCount - 1).requestFocus()
            }
        }

        addIcmpButton.setOnClickListener {
            val model = IcmpData(null, null, ContentEncoding.RAW, null)
            icmpAdapter.addItem(model)
            scrollView.post {
                scrollView.fullScroll(View.FOCUS_DOWN)
                recyclerIcmpView.getChildAt(icmpAdapter.itemCount - 1).requestFocus()
            }
        }

        sequenceTypeGroup.onToggledListener = { toggle, selected ->
            if (selected) {
                mainViewModel.getDirtySequence().value?.type = if (toggle.id == R.id.type_icmp) {
                    KnockType.ICMP
                } else {
                    KnockType.PORT
                }
                showSequence(mainViewModel.getDirtySequence().value?.type ?: KnockType.PORT)
            }
        }
    }

    private fun showSequence(type: KnockType) {
        if (type == KnockType.ICMP) {
            portsLayout.visibility = View.GONE
            icmpLayout.visibility = View.VISIBLE
        } else {
            icmpLayout.visibility = View.GONE
            portsLayout.visibility = View.VISIBLE

        }

    }

}