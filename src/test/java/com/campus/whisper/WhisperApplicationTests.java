package com.campus.whisper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WhisperApplicationTests {

    static {
        WhisperApplication.loadDotEnv();
    }

	@Test
	void contextLoads() {
	}

}
