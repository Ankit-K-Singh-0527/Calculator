package com.example.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.calculator.databinding.ActivityMainBinding
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val engine = CalculatorEngine()
    private var expression: String = ""
    private var lastResult: Double = 0.0
    private var showingResult = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDegRadToggle()
        wireButtons()
        updateDisplay()
    }

    private fun setupDegRadToggle() {
        val t = binding.degRadToggle
        t.setOnClickListener {
            val newMode =
                if (t.text.toString() == getString(R.string.degree)) {
                    t.setText(R.string.radian)
                    CalculatorEngine.AngleMode.RAD
                } else {
                    t.setText(R.string.degree)
                    CalculatorEngine.AngleMode.DEG
                }
            engine.setAngleMode(newMode)
        }
        engine.setAngleMode(CalculatorEngine.AngleMode.DEG)
    }

    private fun wireButtons() = with(binding) {
        val map = mapOf(
            btn0 to "0", btn1 to "1", btn2 to "2", btn3 to "3", btn4 to "4",
            btn5 to "5", btn6 to "6", btn7 to "7", btn8 to "8", btn9 to "9",
            btnDot to ".", btnAdd to "+", btnSub to "-", btnMul to "×",
            btnDiv to "÷", btnPow to "^", btnLParen to "(", btnRParen to ")",
            btnSqrt to "√", btnSin to "sin(", btnCos to "cos(", btnTan to "tan(",
            btnLog to "log(", btnLn to "ln(", btnPi to "π", btnE to "e",
            btnFact to "!", btnPercent to "%", btnExp to "e",
            btnAns to "Ans"
        )
        map.forEach { (b, s) -> b.setOnClickListener { appendToken(s) } }

        btnNeg.setOnClickListener { toggleNegation() }
        btnAC.setOnClickListener { clearAll() }
        btnDel.setOnClickListener { backspace() }
        btnEq.setOnClickListener { evaluateNow() }
        btnCopy.setOnClickListener { copyResult() }
    }

    private fun appendToken(token: String) {
        if (showingResult && token.firstOrNull()?.isDigit() == true) {
            expression = ""
            showingResult = false
        }
        expression += token
        updateDisplay()
    }

    private fun toggleNegation() {
        expression = if (expression.isEmpty()) "-(" else "$expression*(-1)"
        updateDisplay()
    }

    private fun clearAll() {
        expression = ""
        binding.resultView.text = "0"
        showingResult = false
        updateDisplay()
    }

    private fun backspace() {
        if (expression.isNotEmpty()) {
            expression = expression.dropLast(1)
            updateDisplay()
        }
    }

    private fun evaluateNow() {
        val exprToEval = expression
            .replace("Ans", CalculatorEngine.lastAnswer.toString())
            .replace("%", "/100")

        val res = engine.evaluate(exprToEval)
        if (res.error == null) {
            val rounded = roundSmart(res.value)
            lastResult = rounded
            CalculatorEngine.lastAnswer = rounded
            binding.resultView.text = formatResult(rounded)
            showingResult = true
        } else {
            binding.resultView.text = res.error
            showingResult = true
        }
    }

    private fun roundSmart(x: Double): Double {
        if (x.isNaN() || x.isInfinite()) return x
        val s = "%,.12g".format(x).replace(",", "")
        return s.toDoubleOrNull() ?: x
    }

    private fun formatResult(x: Double): String {
        if (x.isNaN() || x.isInfinite()) return x.toString()
        val absx = abs(x)
        return if (absx != 0.0 && (absx < 1e-6 || absx > 1e9)) {
            "%1.10e".format(x)
        } else {
            val raw = "%,.10f".format(x).replace(",", "")
            raw.trimEnd('0').trimEnd('.')
        }
    }

    private fun copyResult() {
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("result", binding.resultView.text)
        cm.setPrimaryClip(clip)
    }

    private fun updateDisplay() {
        binding.expressionView.text = expression
        if (!showingResult) binding.resultView.text = "0"
    }
}