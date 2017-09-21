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

/**
 * Created by minjaesong on 2017-09-18.
 */
object TaskMain : Screen {

    private lateinit var playerTex: Texture

    private val gradTopCol = Color(0x1f629eff)
    private val gradBottomCol = Color(0xe8feffff.toInt())

    private lateinit var batch: SpriteBatch
    private lateinit var shapeRenderer: ShapeRenderer


    var playerPosX = Gdx.graphics.width / 2f
    var playerPosY = Gdx.graphics.height / 2f


    private var runStage = 0 // 0: Not started, 1: First phase, 2: Second phase, 3: Third phase, 4: Result
    val RUNSTAGE_RESULT = 4
    private lateinit var runTimes: FloatArray
    private var timer = 0f


    private val moveScale = 1.333f
    private var lissTheta = 0.0


    private lateinit var font: GameFontBase
    private lateinit var ditherShader: ShaderProgram

    private lateinit var fullscreenQuad: Mesh

    init {
        resetGame()


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
        playerPosX = Gdx.graphics.width / 2f
        playerPosY = Gdx.graphics.height / 2f
        lissTheta = 0.0
        timer = 0f
        rndBit = Random().nextInt(8)
    }

    override fun hide() {
        dispose()
    }

    override fun show() {
        batch = SpriteBatch()
        shapeRenderer = ShapeRenderer()

        Gdx.input.inputProcessor = ConflictInputProcessor()

        font = GameFontBase("assets/fonts")

        ditherShader = ShaderProgram(Gdx.files.internal("assets/dither.vert"), Gdx.files.internal("assets/dither.frag"))


        if (!ditherShader.isCompiled) {
            Gdx.app.log("shaderBayer", ditherShader.log)
            System.exit(1)
        }


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

        rndBit = Random().nextInt(8)
    }

    private val slownessFactor = 2.0
    private var rndBit = 0 // 0bxxx / when updating position: 0: -=, 1: +=

    override fun render(delta: Float) {
        fun proceedToNextStage() {
            if (timer >= runTimes[runStage]) {
                runStage += 1
                timer = 0f
            }
        }

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

        }
        // TASK mode
        else {

            ditherShader.begin()
            ditherShader.setUniformMatrix("u_projTrans", batch.projectionMatrix)
            ditherShader.setUniformf("bottomColor", gradTopCol.r, gradTopCol.g, gradTopCol.b)
            ditherShader.setUniformf("topColor", gradBottomCol.r, gradBottomCol.g, gradBottomCol.b)
            ditherShader.setUniformf("parallax", playerPosY / Gdx.graphics.height * 2f - 1f) // -1 .. 1
            ditherShader.setUniformf("parallax_size", 0.14f)
            fullscreenQuad.render(ditherShader, GL20.GL_TRIANGLES)
            ditherShader.end()


            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0)

            batch.inUse {
                // draw objects
                batch.draw(playerTex, (playerPosX - playerTex.width / 2), (playerPosY - playerTex.height / 2))
            }


            // UPDATE
            if (runStage == 1) {
                playerPosX += (if (rndBit and 1 == 0) 1 else -1) *
                        moveScale * Math.cos((timer - runTimes[runStage]).toDouble() / slownessFactor).toFloat()

                proceedToNextStage()
            }
            else if (runStage == 2) {
                //playerPosX = Gdx.graphics.width / 2f
                playerPosY += (if (rndBit and 1 == 0) 1 else -1) *
                        moveScale * Math.cos((timer - runTimes[runStage]).toDouble() / slownessFactor).toFloat()

                proceedToNextStage()

            }
            else if (runStage == 3) {
                // TODO lissajous
                playerPosX += (if (rndBit and 1 == 0) 1 else -1) *
                        moveScale * Math.sin((timer - runTimes[runStage]).toDouble() / slownessFactor).toFloat()
                playerPosY += (if (rndBit and 1 == 0) 1 else -1) *
                        moveScale * Math.cos((timer - runTimes[runStage]).toDouble() / slownessFactor).toFloat()

                proceedToNextStage()
            }



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

        ditherShader.dispose()
    }

    class ConflictInputProcessor : InputProcessor {
        private var oldMouseX = -2
        private var oldMouseY = -2

        override fun touchUp(p0: Int, p1: Int, p2: Int, p3: Int): Boolean {
            return false
        }

        override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
            if (runStage > 0) {
                if (oldMouseX == -2) oldMouseX = screenX // init, -1 is an placeholder value
                if (oldMouseY == -2) oldMouseY = screenY // init, -1 is an placeholder value

                playerPosX -= (oldMouseX - screenX).toFloat()
                playerPosY += (oldMouseY - screenY).toFloat()

                oldMouseX = screenX
                oldMouseY = screenY
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
            if (runStage == 0) runStage = 1

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

data class ControlDataPoints(val x: Float, val y: Float)