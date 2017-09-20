package net.torvald.conflict;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import net.torvald.terrarum.langpack.Lang;

/**
 * Reference: http://www.mindreadings.com/ControlDemo/Conflict.html
 *
 *
 * This program will run optimally on decent machine, which can maintain
 * constant 60 frames per second.
 *
 * Created by minjaesong on 2017-09-18.
 */
public class Conflict extends Game {

    public static LwjglApplicationConfiguration appConfig;

    public static final String sysLang = System.getProperty("user.language") + System.getProperty("user.country");

    public static void main(String[] args) {
        appConfig = new LwjglApplicationConfiguration();

        appConfig.width = 960;
        appConfig.height = 768;
        appConfig.foregroundFPS = 60;
        appConfig.backgroundFPS = 60;
        appConfig.resizable = false;
        appConfig.title = "Conflict! — " + Lang.INSTANCE.get("MENU_IO_LOADING");

        new LwjglApplication(new Conflict(), appConfig);
    }


    @Override
    public void create() {
        Gdx.graphics.setTitle("Conflict! — " + Lang.INSTANCE.get("MENU_IO_LOADING"));

        setScreen(TaskMain.INSTANCE);
    }
}
