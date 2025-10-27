package com.example.calculator
import kotlin.math.*
class CalculatorEngine {

    enum class AngleMode { DEG, RAD }
    data class EvalResult(val value: Double, val error: String? = null)

    private var mode: AngleMode = AngleMode.DEG
    fun setAngleMode(m: AngleMode) { mode = m }

    fun evaluate(expr: String): EvalResult {
        return try {
            val tokens = tokenize(preprocess(expr))
            val parser = Parser(tokens, mode)
            val value = parser.parseExpression()
            if (parser.hasMore()) throw IllegalArgumentException("Unexpected token")
            EvalResult(value)
        } catch (e: Exception) {
            EvalResult(Double.NaN, e.message ?: "Invalid expression")
        }
    }

    private fun preprocess(s: String): String {
        return s.replace("×", "*")
            .replace("÷", "/")
            .replace("√", "sqrt")
            .replace("π", "pi")
            .replace("Ans", "Ans")
    }

    private enum class TType { NUM, ID, OP, LPAREN, RPAREN, FACT, EOF }
    private data class Token(val type: TType, val text: String)

    private fun tokenize(s: String): List<Token> {
        val out = mutableListOf<Token>()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    var hasE = false
                    i++
                    while (i < s.length) {
                        val ch = s[i]
                        if (ch.isDigit() || ch == '.') { i++ }
                        else if ((ch == 'e' || ch == 'E') && !hasE) {
                            hasE = true; i++
                            if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
                        } else break
                    }
                    out.add(Token(TType.NUM, s.substring(start, i)))
                }
                c.isLetter() -> {
                    val start = i
                    i++
                    while (i < s.length && (s[i].isLetter() || s[i].isDigit())) i++
                    out.add(Token(TType.ID, s.substring(start, i)))
                }
                c == '(' -> { out.add(Token(TType.LPAREN, "(")); i++ }
                c == ')' -> { out.add(Token(TType.RPAREN, ")")); i++ }
                c == '!' -> { out.add(Token(TType.FACT, "!")); i++ }
                "+-*/^%".contains(c) -> { out.add(Token(TType.OP, "$c")); i++ }
                else -> throw IllegalArgumentException("Bad char: '$c'")
            }
        }
        out.add(Token(TType.EOF, ""))
        return out
    }

    private class Parser(val tokens: List<Token>, val mode: AngleMode) {
        var pos = 0
        private fun peek() = tokens[pos]
        private fun match(type: TType, text: String? = null): Boolean {
            val t = peek()
            if (t.type == type && (text == null || t.text == text)) { pos++; return true }
            return false
        }
        fun hasMore() = peek().type != TType.EOF

        fun parseExpression(): Double {
            var v = parseTerm()
            while (true) {
                when {
                    match(TType.OP, "+") -> v += parseTerm()
                    match(TType.OP, "-") -> v -= parseTerm()
                    else -> return v
                }
            }
        }

        private fun parseTerm(): Double {
            var v = parsePower()
            while (true) {
                when {
                    match(TType.OP, "*") -> v *= parsePower()
                    match(TType.OP, "/") -> {
                        val r = parsePower()
                        if (r == 0.0) throw ArithmeticException("Division by zero")
                        v /= r
                    }
                    match(TType.OP, "%") -> {
                        val r = parsePower()
                        v %= r
                    }
                    else -> return v
                }
            }
        }

        private fun parsePower(): Double {
            var v = parseUnary()
            if (match(TType.OP, "^")) {
                val r = parsePower()
                v = v.pow(r)
            }
            return v
        }

        private fun parseUnary(): Double {
            return when {
                match(TType.OP, "+") -> +parseUnary()
                match(TType.OP, "-") -> -parseUnary()
                else -> parsePostfix()
            }
        }

        private fun parsePostfix(): Double {
            var v = parsePrimary()
            while (match(TType.FACT)) {
                if (v < 0 || v > 170) throw IllegalArgumentException("n! out of range")
                v = factorial(v)
            }
            return v
        }

        private fun parsePrimary(): Double {
            val t = peek()
            return when (t.type) {
                TType.NUM -> { pos++; t.text.toDouble() }
                TType.ID -> {
                    pos++
                    val id = t.text.lowercase()
                    when (id) {
                        "pi" -> Math.PI
                        "e" -> Math.E
                        "ans" -> lastAns
                        "sqrt" -> sqrt(expectParenArg())
                        "sin" -> sinA(expectParenArg())
                        "cos" -> cosA(expectParenArg())
                        "tan" -> tanA(expectParenArg())
                        "log" -> {
                            val v = expectParenArg()
                            if (v <= 0.0) throw IllegalArgumentException("log domain error")
                            log10(v)
                        }
                        "ln" -> {
                            val v = expectParenArg()
                            if (v <= 0.0) throw IllegalArgumentException("ln domain error")
                            ln(v)
                        }
                        else -> throw IllegalArgumentException("Unknown id: $id")
                    }
                }
                TType.LPAREN -> {
                    pos++
                    val v = parseExpression()
                    if (!match(TType.RPAREN)) throw IllegalArgumentException("Missing )")
                    v
                }
                else -> throw IllegalArgumentException("Unexpected token")
            }
        }

        private fun expectParenArg(): Double {
            if (!match(TType.LPAREN)) throw IllegalArgumentException("Missing (")
            val v = parseExpression()
            if (!match(TType.RPAREN)) throw IllegalArgumentException("Missing )")
            return v
        }

        private fun toRad(x: Double) = if (mode == AngleMode.DEG) Math.toRadians(x) else x
        private fun sinA(x: Double) = kotlin.math.sin(toRad(x))
        private fun cosA(x: Double) = kotlin.math.cos(toRad(x))
        private fun tanA(x: Double) = kotlin.math.tan(toRad(x))

        private fun factorial(x: Double): Double {
            if (x % 1.0 != 0.0) throw IllegalArgumentException("n! needs integer")
            var r = 1.0
            var n = x.toInt()
            while (n > 1) { r *= n; n-- }
            return r
        }

        companion object { var lastAns: Double = 0.0 }
    }

    companion object {
        var lastAnswer: Double
            get() = Parser.lastAns
            set(value) { Parser.lastAns = value }
    }
}