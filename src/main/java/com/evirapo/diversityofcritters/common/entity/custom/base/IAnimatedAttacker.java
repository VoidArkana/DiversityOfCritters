package com.evirapo.diversityofcritters.common.entity.custom.base;

public interface IAnimatedAttacker {

    boolean isAttacking();

    void setAttacking(boolean attacking);


    int attackAnimationTimeout();

    void setAttackAnimationTimeout(int attackAnimationTimeout);

}