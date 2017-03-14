/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.internal.wire.timer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireSupport;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TimerTest {

    @Test
    public void testActivateSimple() throws SchedulerException, NoSuchFieldException {
        WireHelperService mockWireHelperService = mock(WireHelperService.class);
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Timer timer = new Timer() {
            
            @Override
            protected Scheduler createScheduler() throws SchedulerException {
                return mockScheduler;
            }
        };
        
        timer.bindWireHelperService(mockWireHelperService);

        WireSupport mockWireSupport = mock(WireSupport.class);
        when(mockWireHelperService.newWireSupport(timer)).thenReturn(mockWireSupport);

        String expectedType = "SIMPLE";
        int expectedInterval = 3;
        Map<String, Object> properties = new HashMap<>();
        properties.put("type", expectedType);
        properties.put("simple.interval", expectedInterval);
        
        doAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();

            assertEquals(2, arguments.length);

            JobDetail job = (JobDetail) arguments[0];
            Trigger trigger = (Trigger) arguments[1];
            
            assertTrue(job.getJobDataMap() instanceof TimerJobDataMap);
            assertTrue(trigger instanceof SimpleTriggerImpl);
            
            SimpleTriggerImpl simpleTriggerImpl = (SimpleTriggerImpl) trigger;
            assertEquals((long) expectedInterval * 1000, simpleTriggerImpl.getRepeatInterval());
            
            return null;
        }).when(mockScheduler).scheduleJob(any(), any());
        
        timer.activate(null, properties);

        assertEquals(mockWireSupport, TestUtil.getFieldValue(timer, "wireSupport"));
        
        TimerOptions timerOptions = (TimerOptions) TestUtil.getFieldValue(timer, "timerOptions");
        assertEquals(expectedType, timerOptions.getType());
        assertEquals(expectedInterval, timerOptions.getSimpleInterval());

        assertEquals(mockScheduler, TestUtil.getFieldValue(timer, "scheduler"));
        assertNotNull(TestUtil.getFieldValue(timer, "jobKey"));
        
        verify(mockScheduler).start();
        verify(mockScheduler).scheduleJob(any(), any());
    }

    @Test
    public void testActivateCron() throws SchedulerException, NoSuchFieldException {
        WireHelperService mockWireHelperService = mock(WireHelperService.class);
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Timer timer = new Timer() {
            
            @Override
            protected Scheduler createScheduler() throws SchedulerException {
                return mockScheduler;
            }
        };
        
        timer.bindWireHelperService(mockWireHelperService);

        WireSupport mockWireSupport = mock(WireSupport.class);
        when(mockWireHelperService.newWireSupport(timer)).thenReturn(mockWireSupport);
        
        SchedulerContext mockSchedulerContext = mock(SchedulerContext.class);
        when(mockScheduler.getContext()).thenReturn(mockSchedulerContext);

        String expectedType = "CRON";
        String expectedCronExpression = "0 15 10 ? * *";
        Map<String, Object> properties = new HashMap<>();
        properties.put("type", expectedType);
        properties.put("cron.interval", expectedCronExpression);
        
        doAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();

            assertEquals(2, arguments.length);

            JobDetail job = (JobDetail) arguments[0];
            Trigger trigger = (Trigger) arguments[1];
            
            assertTrue(job.getJobDataMap() instanceof TimerJobDataMap);
            assertTrue(trigger instanceof CronTriggerImpl);
            
            CronTriggerImpl simpleTriggerImpl = (CronTriggerImpl) trigger;
            assertEquals(expectedCronExpression, simpleTriggerImpl.getCronExpression());
            
            return null;
        }).when(mockScheduler).scheduleJob(any(), any());
        
        timer.activate(null, properties);

        assertEquals(mockWireSupport, TestUtil.getFieldValue(timer, "wireSupport"));
        
        TimerOptions timerOptions = (TimerOptions) TestUtil.getFieldValue(timer, "timerOptions");
        assertEquals(expectedType, timerOptions.getType());
        assertEquals(expectedCronExpression, timerOptions.getCronExpression());

        assertEquals(mockScheduler, TestUtil.getFieldValue(timer, "scheduler"));
        assertNotNull(TestUtil.getFieldValue(timer, "jobKey"));
        
        verify(mockScheduler).getContext();
        verify(mockSchedulerContext).put("wireSupport", mockWireSupport);
        verify(mockScheduler).start();
        verify(mockScheduler).scheduleJob(any(), any());
    }

}
