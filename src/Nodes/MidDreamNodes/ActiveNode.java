package Nodes.MidDreamNodes;

import ScriptClasses.MarkovNodeExecutor;
import ScriptClasses.Paint.ScriptStatusPainter;
import ScriptClasses.Util.Statics;
import org.osbot.rs07.api.Prayer;
import org.osbot.rs07.api.ui.*;
import org.osbot.rs07.script.MethodProvider;
import org.osbot.rs07.script.Script;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

/*
    ActiveNode node player flicks. It emulates a player more actively checking to ensure that his account is taking as minimum absorption damage as possible
    It does this by prayer-flicking
*/

public class ActiveNode extends MidDreamNode {
    private boolean doPrayerFlick = true;
    private static MarkovNodeExecutor.ExecutableNode singleton = null;

    private ActiveNode(Script hostScriptReference){
        super(hostScriptReference);
    }

    public static MarkovNodeExecutor.ExecutableNode getSingleton(Script hostScriptReference) {
        if(singleton == null){
            singleton = new ActiveNode(hostScriptReference);
        }
        return singleton;
    }

    /*
    This is called when AFKNode switches back to ActiveNode
     */
    public int resumeActiveNode() throws InterruptedException {
        doPrayerFlick = true;
        onLoopsB4Switch = 100;
        return executeNode();
    }

    @Override
    public int executeNode() throws InterruptedException {
        ScriptStatusPainter.setCurrentMarkovStatus(ScriptStatusPainter.MarkovStatus.ACTIVE_NODE);
        overloadFailSafe();
        checkAbsorption();
        if(hostScriptReference.getSkills().getDynamic(Skill.HITPOINTS) > 1){
            if(!checkOverload()){
                decreaseHP();
            }
        }

        rapidHealFlick(); //rapid heal only flicks if doPrayerFlick variable is true, else it does nothing
        if(!doPrayerFlick && ThreadLocalRandom.current().nextBoolean() && !powerSurgeActive){
            hostScriptReference.getMouse().moveOutsideScreen();
        }

        ScriptStatusPainter.setCurrentScriptStatus(ScriptStatusPainter.ScriptStatus.AFKING);
        onLoopsB4Switch--;
        ScriptStatusPainter.setOnLoopsB4Switch(onLoopsB4Switch);
        return (int) Statics.randomNormalDist(1000, 500);
    }

    @Override
    public boolean doConditionalTraverse() {
        return onLoopsB4Switch <= 0;
    }


    private void rapidHealFlick() throws InterruptedException {
        if(doPrayerFlick){
            ScriptStatusPainter.setCurrentScriptStatus(ScriptStatusPainter.ScriptStatus.RAPID_HEAL_FLICK);
            int currentHealth = hostScriptReference.getSkills().getDynamic(Skill.HITPOINTS);
            //(currentHealth <= 49 || !doesPlayerHaveOverloadsLeft()) player still guzzles to 1 if over 49 and overloads are not in inventory
            if(currentHealth > 1 && (currentHealth <= 49 || !doesPlayerHaveOverloadsLeft())){
                decreaseHP();
            }

            if(currentHealth == 1){
                int currentPrayerPts = hostScriptReference.getSkills().getDynamic(Skill.PRAYER);
                if(currentPrayerPts > 0){
                    //hostScriptReference.log("PRAYER FLICK: flicking prayer, doPrayerFlick -> false");
                    Prayer prayer = hostScriptReference.getPrayer();
                    prayer.open();
                    prayer.set(PrayerButton.RAPID_HEAL, true);
                    MethodProvider.sleep(Statics.randomNormalDist(1000, 200));
                    prayer.set(PrayerButton.RAPID_HEAL, false);
                }
            }
            doPrayerFlick = false;
            //schedule a thread to flip doPrayerFlip to true after ~40s
            int nextFlickMs = (int) Statics.randomNormalDist(40000, 3000);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    doPrayerFlick = true;
                }
            }, nextFlickMs);
            ScriptStatusPainter.startPrayerFlickTimer((nextFlickMs+999)/1000);

            if(ThreadLocalRandom.current().nextBoolean()){
                openInventoryTab();
            }
            hostScriptReference.getMouse().moveOutsideScreen();
        }

    }

}
