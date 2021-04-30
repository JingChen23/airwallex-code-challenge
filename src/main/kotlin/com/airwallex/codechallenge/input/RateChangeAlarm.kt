package com.airwallex.codechallenge.input


data class RateChangeAlarm (
        val timestamp: String,
        val currencyPair: String,
        val alert: String,
        val seconds: Long
){
    override fun toString(): String{
        val res = "{ \"timestamp\": ${timestamp}, " +
                "\"currencyPair\": \"${currencyPair}\", " +
                "\"alert\": \"${alert}\""
        if (seconds > 0){
            return  res + ", \"seconds\": ${seconds} }"
        }
        else {
            return res + " }"
        }
    }
}