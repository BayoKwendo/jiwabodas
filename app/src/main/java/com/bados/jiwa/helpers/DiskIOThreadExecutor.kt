package com.bados.jiwa.helpers

import java.util.concurrent.Executor
import java.util.concurrent.Executors

class DiskIOThreadExecutor : Executor {
    private val mDiskIO: Executor
    override fun execute(command: Runnable) {
        mDiskIO.execute(command)
    }

    init {
        mDiskIO = Executors.newSingleThreadExecutor()
    }
}