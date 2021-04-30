package com.airwallex.codechallenge.input

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.stream.Stream
import kotlin.streams.toList

internal class RateChangeAlarmTest {

    @Test
    fun `when seconds is not 0`() {
        val rca = RateChangeAlarm("1.123", "AUDCNY",
                "spot change", 123)
        assertThat(rca.toString()).isEqualTo(
            "{ \"timestamp\": 1.123, \"currencyPair\": \"AUDCNY\"," +
                    " \"alert\": \"spot change\", \"seconds\": 123 }"
        )
    }

    @Test
    fun `when seconds is 0`() {
        val rca = RateChangeAlarm("1.123", "AUDCNY",
                "spot change", 0)
        assertThat(rca.toString()).isEqualTo(
                "{ \"timestamp\": 1.123, \"currencyPair\": \"AUDCNY\"," +
                        " \"alert\": \"spot change\" }"
        )
    }
}