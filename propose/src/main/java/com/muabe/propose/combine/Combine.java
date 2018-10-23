package com.muabe.propose.combine;

import android.util.Log;

import java.util.ArrayList;

/**
 *
 */
public class Combine {
    public static final int ELEMENT = 0;
    public static final int AND = 1;
    public static final int OR = 2;

    private Combine(){}

    public static <T extends Combination>T all(T... combinations){
        return combine(Combine.AND, combinations);
    }

    public static <T extends Combination>T one(T... combinations){
        return combine(Combine.OR, combinations);
    }

    private static <T extends Combination>T combine(int mode, T... combinations){
        if(combinations.length>0) {
            T newCombination;
            try {
                newCombination = (T)combinations[0].getClass().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            newCombination.mode = mode;
            for (T combination : combinations) {
                combination.parents = newCombination;
                newCombination.child.add(combination);
            }
            return newCombination;
        }else{
            return null;
        }
    }

    public static int count = 0;

    public static <T extends Combination>ArrayList<T> scan(T combination){
        count = 0;

        ArrayList<T> list = new ArrayList<>();
        scanLoop(combination, (ArrayList<Combination>)list);
        for(Combination cacheCombination : list){
            addCache(cacheCombination);
        }

        Log.e("dd", "[검색 횟수:"+count+"]");
        return list;
    }

//    private static void scanLoop1(Combination combination, ArrayList<Combination> list){
//        if(combination.deletedCache){
//            combination.deletedCache = false;
//            return;
//        }
//        count++;
//        switch(combination.mode) {
//            case Combine.ELEMENT:
//                if(combination.compare() > 0) {
//                    list.add(combination);
//                }else{
//                    updateCache(combination, list);
//                }
//                break;
//
//            case Combine.AND:
//                for (Combination childCombine : combination.child) {
//                    scanLoop(childCombine, list);
//                }
//                break;
//            case Combine.OR:
//                ArrayList<Combination> winner = null;
//                if(combination.cache.size()>0){
//                    winner = new ArrayList<>();
//                    scanLoop(combination.cache.get(0), winner);
//                }else{
//                    for (Combination childCombination : combination.child) {
//                        if (winner == null) {
//                            winner = new ArrayList<>();
//                            scanLoop(childCombination, winner);
//                        } else {
//                            ArrayList<Combination> challener = new ArrayList<>();
//                            scanLoop(childCombination, challener);
//
//                            float winnerScore = score(winner);
//                            float challenerScore = score(challener);
//                            if (challenerScore > 0 && winnerScore < challenerScore) {
//                                winner = challener;
//                            }
//                        }
//                    }
//                }
//                list.addAll(winner);
//                break;
//        }
//    }

    private static void scanLoop(Combination combination, ArrayList<Combination> list){
        if(combination.deletedCache){
            combination.deletedCache = false;
            return;
        }
        count++;
        switch(combination.mode) {
            case Combine.ELEMENT:
                if(combination.compare() > 0) {
                    list.add(combination);
                }else{
                    updateCache(combination, list);
                }
                break;

            case Combine.AND:
                for (Combination childCombine : combination.child) {
                    scanLoop(childCombine, list);
                }
                break;
            case Combine.OR:
                ArrayList<Combination> winner = null;
                if(combination.cache.size()>0){
                    winner = new ArrayList<>();
                    scanLoop(combination.cache.get(0), winner);
                }else{
                    for (Combination childCombination : combination.child) {
                        if (winner == null) {
                            winner = new ArrayList<>();
                            scanLoop(childCombination, winner);
                        } else {
                            ArrayList<Combination> challener = new ArrayList<>();
                            scanLoop(childCombination, challener);

                            float[] winnerScore = score(winner);
                            float[] challenerScore = score(challener);

                            if(challenerScore[1] > 0) {
                                if(winnerScore[1] > 0 ) {
                                    if (winnerScore[0] < challenerScore[0] || (winnerScore[0] == challenerScore[0] && winnerScore[1] < challenerScore[1])) {
                                        winner = challener;
                                    }
                                }else{
                                    winner = challener;
                                }
                            }
                        }
                    }
                }
                list.addAll(winner);
                break;
        }
    }

    /**
     * 부모에 캐쉬가 등록되어 있지 않으면 캐쉬를 부모에 등록하고
     * 또 다시 부모의 부모에 대해서 재귀호출을 하면서 캐쉬를 변경 및 등록해준다.
     * 이미 부모의 캐쉬가 등록되어 있는 상황이라면 캐쉬 등록을 끝마친다.
     * OR일 경우 캐쉬는 한개만 유지
     * @param combination 캐쉬를 등록할 대상
     */
    private static void addCache(Combination combination){
        if(combination.parents!=null){
            if(combination.parents.mode == Combine.OR && !combination.parents.cache.contains(combination)){
                combination.parents.cache.clear();
                combination.parents.cache.add(combination);
            }else if(combination.parents.mode == Combine.AND && combination.parents.cache.size() != combination.parents.child.size()){
                combination.parents.cache.clear();
                combination.parents.cache.addAll(combination.parents.child);
            }else{
                return;
            }
            addCache(combination.parents);
            Log.i("dd", "cache="+combination.parents);
        }
    }

    private static void updateCache(Combination combination, ArrayList<Combination> list){
        Combination reScan = deleteCache(combination);
        if(reScan.mode != Combine.ELEMENT) {
            Log.e("dd", "재스캔!! = "+reScan.toString());
            scanLoop(reScan, list);
        }
    }

    /**
     * 부모의 캐쉬에 이미 등록되지 않은 상태라면 빠져나간다.
     * 캐쉬가 등록되어 있을 경우 캐쉬를 삭제하고 부모의 캐쉬가 하나도 남지 않으면
     * 부모의 부모를 재귀호출을 하면서 캐쉬의 삭제를 진행한다.
     * @param combination 캐쉬를 삭제할 대상
     */
    private static Combination deleteCache(Combination combination){
        if(combination.parents!=null && combination.parents.cache.contains(combination)){
            combination.parents.cache.remove(combination);
            Log.i("dd", combination.parents+" delete="+combination);
            if(combination.parents.cache.size()==0){
                if(combination.parents.mode == Combine.OR && combination.mode != Combine.OR){
                    combination.deletedCache = true;
                }
                return deleteCache(combination.parents);
            }
        }
        return combination;
    }


    private static void updateCascheBottomTop(Combination combination, ArrayList<Combination> list){
        if(combination.parents!=null && combination.parents.cache.contains(combination)){
            combination.parents.cache.remove(combination);
            Log.i("dd", combination.parents+" delete="+combination);
            if(combination.parents.cache.size()==0){
                if(combination.parents.mode == Combine.OR){
                    combination.deletedCache = true;
                    ArrayList<Combination> tempList = new ArrayList<>();
                    scanLoop(combination.parents, tempList);
                    if(tempList.size()==0){
                        updateCascheBottomTop(combination.parents, list);
                    }else{
                        list.addAll(tempList);
                    }
                }else if(combination.parents.mode == Combine.AND){
                    updateCascheBottomTop(combination.parents, list);
                }
            }
        }
    }


    public static void clearCache(Combination combination){
        combination.cache.clear();
        if(combination.mode != Combine.ELEMENT) {
            for (Combination childCombine : combination.child) {
                clearCache(childCombine);
            }
        }
    }


    /**
     * 중요!! 모든 리스트는 Element로 구성되어야함
     * @param list mode가 전부 Element 이여야함
     * @return combination의 compare 값
     */
    private static float score1(ArrayList<Combination> list){
        int i = 0;
        float score = -1f;
        int priority = 0;
        for(Combination combination : list){
            if(i==0){
                priority = combination.getPriority();
                score = combination.compare();
                i++;
            }else{
                if(score < combination.compare()){
                    score = combination.compare();
                }
            }
        }
        return score;
    }

    private static float[] score(ArrayList<Combination> list){
        int i = 0;
//        float score = -1f;
//        int getPriority = 0;
        float[] score = {0f, -1f};
        for(Combination combination : list){
            if(i==0){
                score[0] = combination.getPriority();
                score[1] = combination.compare();
                i++;
            }else{
                int tempPriority = combination.getPriority();
                float tempCompare = combination.compare();

                if(tempCompare > 0) {
                    if (score[0] < tempPriority) {
                        score[0] = tempPriority;
                        score[1] = tempCompare;
                    } else if (score[0] > tempPriority) {
                        if(score[1] == 0) {
                            score[0] = tempPriority;
                            score[1] = tempCompare;
                        }
                    } else {
                        if (score[1] < combination.compare()) {
                            score[1] = combination.compare();
                        }
                    }
                }
            }
        }
        return score;
    }
}
