package com.example.wordle

import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordle.data.wordList
import com.example.wordle.data.wordListSize
import com.example.wordle.databinding.FragmentGameScreenBinding
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.*

const val EMPTY_STRING = ""
const val WORD_LENGTH = 5

class WordleViewModel(binding: FragmentGameScreenBinding) : ViewModel() {

    val signal = MutableSharedFlow<Signal>()

    val resetStack: Stack<Pair<TextView, Button?>> = Stack()

    var r = Random(System.nanoTime()).nextInt(wordListSize)
    val sli get() = r * WORD_LENGTH
    val wordle get() =  wordList.slice(sli until sli+ WORD_LENGTH)

    val lettersRow = listOf(
        binding.firstLettersRow,
        binding.secondLettersRow,
        binding.thirdLettersRow,
        binding.fourthLettersRow,
        binding.fifthLettersRow,
        binding.sixthLettersRow
    )

    private val tryRows = listOf(
        listOf(
            binding.firstRow5,
            binding.firstRow4,
            binding.firstRow3,
            binding.firstRow2,
            binding.firstRow1
        ),
        listOf(
            binding.secondRow5,
            binding.secondRow4,
            binding.secondRow3,
            binding.secondRow2,
            binding.secondRow1
        ),
        listOf(
            binding.thirdRow5,
            binding.thirdRow4,
            binding.thirdRow3,
            binding.thirdRow2,
            binding.thirdRow1
        ),
        listOf(
            binding.fourthRow5,
            binding.fourthRow4,
            binding.fourthRow3,
            binding.fourthRow2,
            binding.fourthRow1
        ),
        listOf(
            binding.fifthRow5,
            binding.fifthRow4,
            binding.fifthRow3,
            binding.fifthRow2,
            binding.fifthRow1
        ),
        listOf(
            binding.sixthRow5,
            binding.sixthRow4,
            binding.sixthRow3,
            binding.sixthRow2,
            binding.sixthRow1
        )
    )

    val keyboardMap = mapOf<String, Button>(
        "A" to binding.A,
        "B" to binding.B,
        "C" to binding.C,
        "D" to binding.D,
        "E" to binding.E,
        "F" to binding.F,
        "G" to binding.G,
        "H" to binding.H,
        "I" to binding.I,
        "J" to binding.J,
        "K" to binding.K,
        "L" to binding.L,
        "M" to binding.M,
        "N" to binding.N,
        "O" to binding.O,
        "P" to binding.P,
        "Q" to binding.Q,
        "R" to binding.RR,
        "S" to binding.S,
        "T" to binding.T,
        "U" to binding.U,
        "V" to binding.V,
        "W" to binding.W,
        "X" to binding.X,
        "Y" to binding.Y,
        "Z" to binding.Z
    )

    var `try` = State.TRY1
    var guess = List<String>(WORD_LENGTH) { "" }

    val letterStack = Stack<TextView>()
    val trash = Stack<TextView>()

    fun initLetterStack() {
        letterStack.clear()
        trash.clear()
        val rows = when (`try`) {
            State.TRY1 -> tryRows[0]
            State.TRY2 -> tryRows[1]
            State.TRY3 -> tryRows[2]
            State.TRY4 -> tryRows[3]
            State.TRY5 -> tryRows[4]
            State.TRY6 -> tryRows[5]
            else -> tryRows[0]
        }
        rows.forEach { letterStack.push(it) }
    }

    fun nextState() {
        `try` = when (`try`) {
            State.TRY1 -> State.TRY2
            State.TRY2 -> State.TRY3
            State.TRY3 -> State.TRY4
            State.TRY4 -> State.TRY5
            State.TRY5 -> State.TRY6
            State.TRY6 -> State.TRY1
        }
        initLetterStack()
    }

    private fun getNewWord() {
        var r = Random(System.nanoTime()).nextInt(wordListSize)
        //wordle = wordList.random()
    }

    private fun wordToList(): List<String> {
        return List<String>(WORD_LENGTH) { wordle[it].uppercaseChar().toString() }
    }

    private fun listToWord(l: List<String>, r: String = EMPTY_STRING): String {
        if (l.isEmpty()) {
            return r
        }
        return listToWord(l.drop(1), r + l.first())
    }

    fun check() {
        guess = List<String>(5) {
            tryRows[`try`.id][it].text.toString()
        }.reversed()

        viewModelScope.launch {
            when {
                (guess.filter { it != " " }.size < 5) -> {
                    signal.emit(Signal.NEEDLETTER)
                }
                (guess == wordToList()) -> {
                    signal.emit(Signal.WIN)
                }
                (searchWordList(wordList, listToWord(guess))) -> {
                    if (`try` == State.TRY6) {
                        signal.emit(Signal.GAMEOVER)
                    } else {
                        signal.emit(Signal.NEXTTRY)
                    }
                }
                else -> {
                    signal.emit(Signal.NOTAWORD)
                }
            }
        }
    }

    fun colorLogic(
        g: List<String> = guess,
        w: List<String> = wordToList()): MutableList<Triple<TextView,Button, Int>> {
        val result = mutableListOf<Triple<TextView, Button, Int>>()
        tryRows[`try`.id].reversed().forEachIndexed{ index,  textview ->
            when(g[index]) {
                w[index] -> result.add(Triple(textview, keyboardMap[textview.text]!!, R.color.green))
                in w     -> result.add(Triple(textview, keyboardMap[textview.text]!!, R.color.yellow))
                else     -> result.add(Triple(textview,keyboardMap[textview.text]!!, R.color.dark_gray))
            }
            resetStack.add(Pair(textview, keyboardMap[textview.text.toString()]))
        }
        return result
    }

    fun reset() {
        getNewWord()
        `try` = State.TRY1
        initLetterStack()
    }

    private fun searchWordList(wordList: String, word: String, lo: Int = 0, hi: Int = wordListSize): Boolean {
        if (lo > hi) return false
        var mid = ((hi - lo) / 2) + lo
        val sli = WORD_LENGTH * mid

        val midWord = wordList.slice(sli..sli+4)

        Log.i("HELP", "word is: ${listToWord(guess, EMPTY_STRING)} midWord is: $midWord")
        return when {
            word < midWord  -> searchWordList(wordList, word, lo, mid-1)
            word == midWord -> true
            else            -> searchWordList(wordList, word, mid+1, hi)
        }
    }

}
