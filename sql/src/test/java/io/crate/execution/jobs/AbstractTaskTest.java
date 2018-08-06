/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.execution.jobs;

import com.google.common.collect.ImmutableList;
import io.crate.test.integration.CrateUnitTest;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;

public class AbstractTaskTest extends CrateUnitTest {

    private TestingTask testingTask;

    private Runnable killRunnable = new Runnable() {
        @Override
        public void run() {
            testingTask.kill(null);
        }
    };

    private Runnable closeRunnable = new Runnable() {
        @Override
        public void run() {
            testingTask.close(null);
        }
    };

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        testingTask = new TestingTask();
    }

    private void runAsync(Runnable task, int calls) {
        List<Thread> threads = new ArrayList<Thread>(calls);
        for (int i = 0; i < calls; i++) {
            Thread t = new Thread(task);
            t.start();
            threads.add(t);
        }
        for (Thread thread : threads) {
            try {
                thread.join(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class TestingTask extends AbstractTask {

        private static final Logger LOGGER = Loggers.getLogger(TestingTask.class);

        final AtomicInteger numPrepare = new AtomicInteger();
        final AtomicInteger numStart = new AtomicInteger();
        final AtomicInteger numClose = new AtomicInteger();
        final AtomicInteger numKill = new AtomicInteger();

        TestingTask(int id) {
            super(id, LOGGER);
        }

        public TestingTask() {
            this(0);
        }

        @Override
        public String name() {
            return getClass().getSimpleName();
        }

        @Override
        protected void innerClose(@Nullable Throwable t) {
            numClose.incrementAndGet();
        }

        @Override
        protected void innerKill(@Nonnull Throwable t) {
            numKill.incrementAndGet();
        }

        @Override
        public void innerPrepare() {
            numPrepare.incrementAndGet();
        }

        @Override
        protected void innerStart() {
            numStart.incrementAndGet();
        }

        public List<Integer> stats() {
            return ImmutableList.of(
                numPrepare.get(),
                numStart.get(),
                numClose.get(),
                numKill.get()
            );
        }

    }

    @Test
    public void testNormalSequence() throws Exception {
        TestingTask task = new TestingTask();
        task.prepare();
        task.start();
        task.close(null);
        assertThat(task.stats(), contains(1, 1, 1, 0));
    }

    @Test
    public void testCloseAfterPrepare() throws Exception {
        TestingTask task = new TestingTask();
        task.prepare();
        task.close(null);
        task.start();
        task.close(null);
        assertThat(task.stats(), contains(1, 0, 1, 0));
    }

    @Test
    public void testParallelClose() throws Exception {
        testingTask.prepare();
        testingTask.start();
        runAsync(closeRunnable, 3);
        assertThat(testingTask.stats(), contains(1, 1, 1, 0));
    }

    @Test
    public void testParallelKill() throws Exception {
        testingTask.prepare();
        testingTask.start();
        runAsync(killRunnable, 3);
        assertThat(testingTask.stats(), contains(1, 1, 0, 1));
        assertThat(testingTask.numKill.get(), greaterThan(0));
    }

}
