package com.muabe.propose.guesture;

import com.muabe.propose.util.Mlog;

/**
 * <br>捲土重來<br>
 *
 * @author 오재웅(JaeWoong - Oh)
 * @email markjmind@gmail.com
 * @since 2018-10-18
 */
public class TestGesture extends GesturePlugin<TestEvent> {
    @Override
    public void get(TestEvent testEvent) {
        testEvent.count++;
        Mlog.i(this, "TestGesture:"+testEvent.count);
    }

    @Override
    public float increase(TestEvent event) {
        return 0;
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
