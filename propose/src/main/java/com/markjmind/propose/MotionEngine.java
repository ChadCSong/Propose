package com.markjmind.propose;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * <br>捲土重來<br>
 * MotionEngine 클래스의 원리는 애니메이션의 시간을 거리고 환산하여 그것을 Touch 좌표로 대입하는 일을 한다.<br>
 *
 * @author 오재웅(JaeWoong-Oh)
 * @email markjmind@gmail.com
 * @since 2016-03-28
 */
    public class MotionEngine implements GestureDetector.OnGestureListener{

    protected PointEvent pointEventX, pointEventY;

    private Hashtable<Integer, MotionsInfo> motionMap = new Hashtable<>();
    private ActionState state;
    private boolean isFirstTouch;
    private float density;

    private DetectListener detectListener;

    protected int horizontalPriority = Motion.RIGHT;
    protected int verticalPriority = Motion.UP;

    /** 속성변경 옵션으로 지원할수 있도록 하기**/
    private float maxVelocity = 5f;
    private float minVelocity = 0.5f;
    private float gravity = 0.5f;
    private float maxEndBuffer = 3f;

    protected MotionEngine(ActionState state, float density, DetectListener detectListener){
        this.density = density;
        maxEndBuffer = maxEndBuffer*density;

        pointEventX = new PointEvent(Motion.LEFT, Motion.RIGHT, density);
        pointEventY = new PointEvent(Motion.UP, Motion.DOWN, density);
        this.state = state;
        this.detectListener = detectListener;
        reset();
    }

    protected void reset(){
        isFirstTouch = true;
        state.setState(ActionState.STOP);
    }


    protected void addMotion(int direction, Motion motion){
        MotionsInfo info = new MotionsInfo();
        if (motionMap.containsKey(direction)) {
            info = motionMap.get(direction);
        }
        if(direction == Motion.LEFT || direction == Motion.RIGHT) {
            motion.setPointEvent(pointEventX);
        }else if(direction == Motion.UP || direction == Motion.DOWN){
            motion.setPointEvent(pointEventY);
        }
        info.add(motion);
        motionMap.put(direction, info);

    }

    protected int getActionState() {
        return this.state.getState();
    }


    /****************************************** Gesture ****************************************/

    /******** Down *******/
    @Override
    public boolean onDown(MotionEvent event) {
        isFirstTouch = true;
        pointEventX.setEvent(event.getRawX(), event.getEventTime());
        pointEventX.setVelocity(0f);
        pointEventY.setEvent(event.getRawY(), event.getEventTime());
        pointEventY.setVelocity(0f);
        pointEventX.endBuffer = 0f;
        pointEventY.endBuffer = 0f;
        return false;
    }


    /******** Up *******/
    public boolean onUp(MotionEvent event){
        boolean result = false;
        if(state.getState() == ActionState.SCROLL){
            result =  moveUp(pointEventX);
            result =  moveUp(pointEventY) || result;
            if(!result){
                state.setState(ActionState.STOP);
            }
        }
        return result;
    }

    private boolean moveUp(PointEvent pointEvent){
        Log.i("Detector", "moveUp");
        boolean result = false;
        int direction = pointEvent.getDirection();
        if(direction==0){
            return false;
        }
        MotionsInfo info;
        if(direction != Motion.NONE && (info = motionMap.get(direction)) != null) {
            for (Motion motion : info.motions) {
                boolean forward = motion.getCurrDuration() >= motion.getTotalDuration()*gravity;
                if(forward == motion.isForward()){
                    result = motion.animate() || result;
                }else{
                    if(motion.isForward()) {
                        result = motion.animate(motion.getCurrDuration(), 0) || result;
                    }else{
                        result = motion.animate(motion.getCurrDuration(), motion.getTotalDuration()) || result;
                    }
                }
            }
        }
        return result;
    }

    /******** TapUp *******/
    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        Log.i("Detector", "onSingleTapUp");
        boolean result;
        result = tapUp(pointEventX, horizontalPriority);
        result = tapUp(pointEventY, verticalPriority) || result;
        return result;
    }

    private boolean tapUp(PointEvent pointEvent, int priority){
        boolean result = false;
        int direction = pointEvent.getDirection();
        if(direction==0){
            direction = priority;
        }
        MotionsInfo info;
        if(direction != Motion.NONE && (info = motionMap.get(direction)) != null) {
            for (Motion motion : info.motions) {
                if(motion.isEnableSingleTabUp()) {
                    result = motion.animate() || result;
                }
            }
        }
        return result;
    }

    /******** Scroll *******/
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        boolean result = false;
        if(state.getState() != ActionState.ANIMATION){
            state.setState(ActionState.SCROLL);
            if(isFirstTouch){ //최초 스크롤시
                isFirstTouch = false;
                pointEventX.setEvent(e2.getRawX(), e2.getEventTime());
                pointEventY.setEvent(e2.getRawY(), e2.getEventTime());
                pointEventX.setPreRaw(e2.getRawX());
                pointEventY.setPreRaw(e2.getRawY());
                result = true;
            } else {
                result = scroll(e2.getRawX(), pointEventX, e2.getEventTime());
                result = scroll(e2.getRawY(), pointEventY, e2.getEventTime()) || result;
            }

        }
        return result;
    }

    private boolean scroll(float raw, PointEvent pointEvent, long time){
        float diff = raw - pointEvent.getRaw();
        pointEvent.setEvent(raw, time);
        int direction = pointEvent.getDirection(diff);

        boolean result = false;

        if(direction == Motion.NONE ){
            result = move(pointEvent, pointEvent.plus, 0f);
            result = move(pointEvent, pointEvent.minus, 0f) || result;
        }else{

            int changeDirection = pointEvent.getChangeDirection(diff);
            if(changeDirection!=Motion.NONE){
                result = move(pointEvent, changeDirection, 0f);
            }
            result = move(pointEvent, direction, diff) || result;
        }
        return result;
    }

    private boolean move(PointEvent pointEvent, int direction, float diff){
        MotionsInfo info;
        boolean result = false;
        if((info = motionMap.get(direction)) != null){
            for (Motion motion : info.motions) {
                if(motion.isEnableMove()) {
                    if (Motion.Position.end.equals(motion.getPosition())) {
                        pointEvent.endBuffer = pointEvent.endBuffer + diff;
                        if (pointEvent.endBuffer * motion.getDirectionArg() >= maxEndBuffer / 2) {
                            pointEvent.endBuffer = maxEndBuffer * motion.getDirectionArg() / 2;
                        } else if (pointEvent.endBuffer * motion.getDirectionArg() < -maxEndBuffer) {
                            diff = pointEvent.endBuffer;
                            pointEvent.endBuffer = 0f;
                            result = motion.moveDistance(Math.abs(pointEvent.getPoint() + diff)) || result;
                        }
                    } else {
                        result = motion.moveDistance(Math.abs(pointEvent.getPoint() + diff)) || result;
                    }
                }
            }
        }
        return result;
    }


    /******** Fling *******/
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.i("Detector", "flingX:"+pointEventX.getVelocity()+ " Y:"+pointEventY.getVelocity()) ;
        boolean result = false;
        if(state.getState() == ActionState.SCROLL){
            state.setState(ActionState.FlING);
            float velocity = Math.abs(pointEventX.getVelocity());

            if(velocity < Math.abs(pointEventY.getVelocity())){
                velocity = Math.abs(pointEventY.getVelocity());
            }

            result = fling(pointEventX, velocity);
            result = fling(pointEventY, velocity) || result;
            if(!result){
                state.setState(ActionState.STOP);
            }
        }
        return result;
    }

    private boolean fling(PointEvent pointEvent, float velocity){
        boolean result = false;
        int direction = pointEvent.getDirection();
        if(velocity == 0f || direction == Motion.NONE){
            return false;
        }else if(velocity > maxVelocity){
            velocity = maxVelocity;
        }else if(velocity < minVelocity){
            velocity = minVelocity;
        }
        MotionsInfo info = motionMap.get(direction);
        if(info != null){
            for (Motion motion : info.motions) {
                long start = motion.getCurrDuration();
                long end = motion.getTotalDuration();
                if((pointEvent.plus == direction && pointEvent.getVelocity() < 0) ||
                        (pointEvent.minus == direction && pointEvent.getVelocity() > 0)){
                    end = 0;
                }
                long playTime = (long)(motion.getDistanceToDuration(Math.abs((end-start)))/velocity);

                result = motion.animate(start, end, playTime) || result;
            }
        }
        return result;
    }


    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public void onLongPress(MotionEvent e) {
        Log.i("Detector", "onLongPress");
    }


    /****************************************** interface and inner class ****************************************/

    interface DetectListener {
        boolean detectScroll(Motion motion, PointEvent pointEventX, PointEvent pointEventY);
        boolean detectFling(PointEvent pointEventX, PointEvent pointEventY);
    }

    class MotionsInfo {
        protected long maxDuration=0;
        protected ArrayList<Motion> motions;
        protected MotionsInfo(){
            motions = new ArrayList<>();
        }

        protected void add(Motion motion){
            if(maxDuration < motion.getTotalDuration()){
                maxDuration = motion.getTotalDuration();
            }
            motions.add(motion);
        }
    }
}
