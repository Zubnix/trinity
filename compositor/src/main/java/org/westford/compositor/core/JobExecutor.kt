/*
 * Westford Wayland Compositor.
 * Copyright (C) 2016  Erik De Rijcke
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.westford.compositor.core

import org.freedesktop.jaccall.Pointer
import org.freedesktop.wayland.server.Display
import org.freedesktop.wayland.server.EventLoop
import org.freedesktop.wayland.server.EventSource
import org.freedesktop.wayland.server.jaccall.WaylandServerCore
import org.westford.nativ.glibc.Libc
import javax.inject.Inject
import javax.inject.Singleton
import java.util.LinkedList
import java.util.Optional
import java.util.concurrent.locks.ReentrantLock

@Singleton
class JobExecutor @Inject
internal constructor(private val display: Display,
                     private val pipeR: Int,
                     private val pipeWR: Int,
                     private val libc: Libc) : EventLoop.FileDescriptorEventHandler {

    private val eventNewJobBuffer = Pointer.nref(EVENT_NEW_JOB)
    private val eventFinishedBuffer = Pointer.nref(EVENT_FINISHED)
    private val eventReadBuffer = Pointer.nref(0.toByte())

    private val jobsLock = ReentrantLock()
    private val pendingJobs = LinkedList<Runnable>()
    private var eventSource = Optional.empty<EventSource>()

    fun start() {
        if (!this.eventSource.isPresent) {
            this.eventSource = Optional.of(this.display.eventLoop
                    .addFileDescriptor(this.pipeR,
                            WaylandServerCore.WL_EVENT_READABLE,
                            this))
        } else {
            throw IllegalStateException("Job executor already started.")
        }
    }

    fun fireFinishedEvent() {
        this.libc.write(this.pipeWR,
                this.eventFinishedBuffer.address,
                1)
    }

    fun submit(job: Runnable) {
        try {
            this.jobsLock.lock()
            this.pendingJobs.add(job)
            //wake up event thread
            fireNewJobEvent()
        } finally {
            this.jobsLock.unlock()
        }
    }

    private fun fireNewJobEvent() {
        this.libc.write(this.pipeWR,
                this.eventNewJobBuffer.address,
                1)
    }

    override fun handle(fd: Int,
                        mask: Int): Int {
        val jobs = commit()
        while (this.eventSource.isPresent) {
            if (!handleNextEvent(jobs)) {
                break
            }
        }

        return 0
    }

    private fun commit(): LinkedList<Runnable> {
        var jobs = NO_JOBS
        try {
            this.jobsLock.lock()
            if (!this.pendingJobs.isEmpty()) {
                jobs = LinkedList(this.pendingJobs)
                this.pendingJobs.clear()
            }
        } finally {
            this.jobsLock.unlock()
        }
        return jobs
    }

    private fun handleNextEvent(jobs: LinkedList<Runnable>): Boolean {
        val event = read()
        if (event == EVENT_FINISHED) {
            clean()
            return false
        } else if (event == EVENT_NEW_JOB) {
            jobs.pop()
                    .run()
            return !jobs.isEmpty()
        } else {
            throw IllegalStateException("Got illegal event code " + event)
        }
    }

    private fun read(): Byte {
        this.libc.read(this.pipeR,
                this.eventReadBuffer.address,
                1)
        return this.eventReadBuffer.dref()
    }

    private fun clean() {
        this.libc.close(this.pipeR)
        this.libc.close(this.pipeWR)
        this.eventSource.get()
                .remove()
        this.eventSource = Optional.empty<EventSource>()
    }

    companion object {

        private val EVENT_NEW_JOB: Byte = 1
        private val EVENT_FINISHED: Byte = 0
        private val NO_JOBS = LinkedList<Runnable>()
    }
}