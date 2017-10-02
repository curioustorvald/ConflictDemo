package net.torvald.conflict

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarumsansbitmap.gdx.GameFontBase
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by minjaesong on 2017-09-18.
 */
object TaskMain : Screen {

    private lateinit var playerTex: Texture

    private val gradTopCol = Color(0x1f629eff)
    private val gradBottomCol = Color(0xe8feffff.toInt())

    private lateinit var batch: SpriteBatch
    private lateinit var shapeRenderer: ShapeRenderer

    val halfW: Float; get() = Gdx.graphics.width / 2f
    val halfH: Float; get() = Gdx.graphics.height / 2f

    var playerPosX = halfW
    var playerPosY = halfH


    private var runStage = 0 // 0: Not started, 1: First phase, 2: Second phase, 3: Third phase, 4: Result
    val RUNSTAGE_RESULT = 4
    private var runTimes: FloatArray
    private var timer = 0f


    private val moveScale: Float; get() = Gdx.graphics.height / 4f
    private val moveScaleForXY: Float; get() = Gdx.graphics.height / 9f


    private lateinit var font: GameFontBase

    private lateinit var fullscreenQuad: Mesh

    private var mouseX = -2
    private var mouseY = -2

    private val pollingTime = 0.2f // in seconds
    private var dataCaptureTimer = 0f

    init {
        //resetGame()


        // load stage running times from config file
        val prop = Properties()
        prop.load(Gdx.files.internal("assets/config.txt").reader())

        runTimes = floatArrayOf(0f,
                prop.getProperty("stageone").toInt() / 1000f,
                prop.getProperty("stagetwo").toInt() / 1000f,
                prop.getProperty("stagethree").toInt() / 1000f
        )
    }


    fun resetGame() {
        playerPosX = halfW
        playerPosY = halfH
        timer = 0f
        rndBit = Random().nextInt(16)

        runStage = 0

        dataPoints.clear()
    }

    override fun hide() {
        dispose()
    }

    override fun show() {
        batch = SpriteBatch()
        shapeRenderer = ShapeRenderer()

        Gdx.input.inputProcessor = ConflictInputProcessor(this)

        font = GameFontBase("assets/fonts")


        fullscreenQuad = Mesh(
                true, 4, 6,
                VertexAttribute.Position(),
                VertexAttribute.ColorUnpacked(),
                VertexAttribute.TexCoords(0)
        )

        fullscreenQuad.setVertices(floatArrayOf(
                0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 1f,
                Gdx.graphics.width.toFloat(), 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f,
                Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat(), 0f, 1f, 1f, 1f, 1f, 1f, 0f,
                0f, Gdx.graphics.height.toFloat(), 0f, 1f, 1f, 1f, 1f, 0f, 0f
        ))
        fullscreenQuad.setIndices(shortArrayOf(0, 1, 2, 2, 3, 0))


        playerTex = Texture(Gdx.files.internal("assets/player_by_Yawackhary.tga"))


        resetGame()
    }

    private var rndBit = 0 // 0bxxxx / when updating position: 0: -=, 1: +=
    private var slowdown = 2.0

    private val dataPoints = ArrayList<ControlDataPoints>()

    fun proceedToNextStage() {
        if (timer >= runTimes[runStage] || runStage == 0) {
            runStage += 1
            timer = 0f
            dataCaptureTimer = 0f


            playerPosX = Gdx.graphics.width / 2f
            playerPosY = Gdx.graphics.height / 2f
        }
    }

    fun pollPosition() {
        if (dataCaptureTimer >= pollingTime) {
            dataCaptureTimer -= pollingTime


            dataPoints.add(ControlDataPoints(playerPosX, playerPosY, runStage))
        }
    }

    override fun render(delta: Float) {

        if (runStage == 0) {
            Gdx.gl.glClearColor(.235f, .235f, .235f, 1f)
        }
        else {
            Gdx.gl.glClearColor(.933f, .933f, .933f, 1f)
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_TEXTURE_2D)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        Gdx.graphics.setTitle("Conflict! — FPS: ${Gdx.graphics.framesPerSecond}")


        // MENU mode
        if (runStage == 0) {
            batch.inUse {
                batch.color = Color.WHITE
                val textWidth = font.getWidth(Lang["MENU_LABEL_PLAY"])
                font.draw(batch, Lang["MENU_LABEL_PLAY"], (Gdx.graphics.width - textWidth) / 2f, Gdx.graphics.height / 2f)
            }
        }
        // RESULT mode
        else if (runStage == RUNSTAGE_RESULT) {
            shapeRenderer.inUse {
                dataPoints.forEach {
                    shapeRenderer.color = Color(0x404040ff)
                    shapeRenderer.line(halfW, 0f, halfW, halfH * 2)
                    shapeRenderer.line(0f, halfH, halfW * 2, halfH)


                    shapeRenderer.color = when(it.stage) {
                        1, 2 -> Color(0x00cc50ff)
                        3 -> Color.RED
                        else -> Color(0)
                    }

                    shapeRenderer.rect(it.x - 0.5f, it.y - 0.5f, 3f, 3f)
                }
            }


            batch.inUse {
                batch.color = Color(0xc0c0c0ff.toInt())
                font.draw(batch, " Legend:", 20f, Gdx.graphics.height - 40f)

                batch.color = Color.RED
                font.draw(batch, "- Conflict", 20f, Gdx.graphics.height - 40f - 40f)

                batch.color = Color(0x00ff55ff)
                font.draw(batch, "- Pre-conflict", 20f, Gdx.graphics.height - 40f - 20f)

                val fontWidth = font.getWidth(Lang["MENU_LABEL_RETURN_MAIN"]).ushr(1).shl(1) // ensure even-ness (as in even number)
                font.draw(batch, Lang["MENU_LABEL_RETURN_MAIN"], Gdx.graphics.width.minus(fontWidth) / 2f, 10f)
            }
        }
        // TASK mode
        else {
            shapeRenderer.inUse {
                //draw sky
                shapeRenderer.rect(0f, 0f, halfW * 2, halfH * 2, gradBottomCol, gradBottomCol, gradTopCol, gradTopCol)

                // draw lines
                shapeRenderer.color = Color(0x404040ff)
                shapeRenderer.rect(halfW, 0f, 1f, halfH * 2)
                shapeRenderer.rect(0f, halfH, halfW * 2, 1f)
            }


            batch.inUse {
                // draw objects
                batch.draw(playerTex, (playerPosX - playerTex.width / 2), (playerPosY - playerTex.height / 2))
            }

            // UPDATE
            if (runStage == 1) {
                playerPosX = mouseX + (if (rndBit and 1 == 0) 1 else -1) *
                        moveScale * Math.sin(timer.div(slowdown)).toFloat()
                playerPosY = mouseY.toFloat()

                pollPosition()
                proceedToNextStage()
            }
            else if (runStage == 2) {
                playerPosX = mouseX.toFloat()
                playerPosY = mouseY + (if (rndBit and 10 == 0) 1 else -1) *
                        moveScale * Math.sin(timer.div(slowdown)).toFloat()

                pollPosition()
                proceedToNextStage()

            }
            else if (runStage == 3) {
                playerPosX = mouseX + (if (rndBit and 100 == 0) 1 else -1) *
                        moveScaleForXY * Math.sin(1.0 * timer.div(slowdown)).toFloat()
                playerPosY = mouseY + (if (rndBit and 1000 == 0) 1 else -1) *
                        moveScaleForXY * Math.sin(2.0 * timer.div(slowdown)).toFloat()

                pollPosition()
                proceedToNextStage()
            }

            // log the shits
            if (runStage in 1..3) {

            }


            dataCaptureTimer += delta
            timer += delta
        }


    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun resize(p0: Int, p1: Int) {
    }

    override fun dispose() {
        playerTex.dispose()
        batch.dispose()
        shapeRenderer.dispose()
    }

    class ConflictInputProcessor(val conflict: TaskMain) : InputProcessor {

        override fun touchUp(p0: Int, p1: Int, p2: Int, p3: Int): Boolean {
            return false
        }

        override fun mouseMoved(screenX: Int, screenY: Int): Boolean {

            if (runStage > 0) {
                if (runStage != 3) {
                    conflict.mouseX = screenX
                    conflict.mouseY = Gdx.graphics.height - screenY // flip
                }
                else {
                    val realMouseX = screenX
                    val realMouseY = Gdx.graphics.height - screenY // flip

                    conflict.mouseX = (realMouseX + realMouseY) / 2
                    conflict.mouseY = conflict.mouseX
                }
            }

            return true
        }

        override fun keyTyped(p0: Char): Boolean {
            return false
        }

        override fun scrolled(p0: Int): Boolean {
            return false
        }

        override fun keyUp(p0: Int): Boolean {
            return false
        }

        override fun touchDragged(x: Int, y: Int, pointer: Int): Boolean {
            if (runStage > 0) {
                return mouseMoved(x, y)
            }

            return true
        }

        override fun keyDown(p0: Int): Boolean {
            return false
        }

        override fun touchDown(x: Int, y: Int, pointer: Int, button: Int): Boolean {
            if (runStage == 0){
                conflict.proceedToNextStage()
            }

            // return to main menu
            else if (runStage == RUNSTAGE_RESULT && y >= Gdx.graphics.height - 40) {
                resetGame()
            }

            return true
        }
    }
}


inline fun SpriteBatch.inUse(action: (SpriteBatch) -> Unit) {
    this.begin()
    action.invoke(this)
    this.end()
}

inline fun ShapeRenderer.inUse(action: (ShapeRenderer) -> Unit) {
    this.begin(ShapeRenderer.ShapeType.Filled)
    action.invoke(this)
    this.end()
}

data class ControlDataPoints(val x: Float, val y: Float, val stage: Int)
