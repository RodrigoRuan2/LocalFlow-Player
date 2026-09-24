package com.localflow.player

import org.junit.Assert.*
import org.junit.Test
import com.localflow.player.model.formatTime
import com.localflow.player.model.normalized
import com.localflow.player.model.*
import com.localflow.player.playback.videoPolicy

class LibraryLogicTest {
    @Test fun searchIgnoresAccentsAndCase() { assertEquals("coracao acustico",normalized("CORAÇÃO Acústico")) }
    @Test fun durationHandlesLongVideoAndUnknownValue() { assertEquals("1:02:03",formatTime(3_723_000)); assertEquals("0:00",formatTime(-1)) }
    @Test fun visibleVideoIsNormalUnlessUserChoosesAudio() { assertFalse(videoPolicy(true,true,false,false).disableVideo); assertFalse(videoPolicy(true,true,false,false).pause); assertTrue(videoPolicy(true,true,true,false).disableVideo) }
    @Test fun backgroundVideoHonorsOptOutAndStopsDecoding() { assertTrue(videoPolicy(true,false,false,false).pause); assertTrue(videoPolicy(true,false,false,false).disableVideo); assertFalse(videoPolicy(true,false,false,true).pause) }
    @Test fun musicUnaffectedByVideoSettings() { assertFalse(videoPolicy(false,false,true,false).pause); assertFalse(videoPolicy(false,false,true,false).disableVideo) }
    @Test fun whatsappAudioIsDetectedFromModernAndLegacyFolders() {
        assertTrue(isWhatsAppAudioFolder("Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio"))
        assertTrue(isWhatsAppAudioFolder("WhatsApp/Media/WhatsApp Voice Notes"))
        assertFalse(isWhatsAppAudioFolder("Music/Downloads"))
    }
}
