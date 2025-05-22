package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.animal.Animal;
import org.checkerframework.checker.units.qual.A;

public class CustomFloatGoal extends FloatGoal {
    public DiverseCritter mob;

    public CustomFloatGoal(DiverseCritter mob) {
        super(mob);
        this.mob = mob;
    }

    public void start() {
        super.start();
        this.mob.clearStates();
    }
}
