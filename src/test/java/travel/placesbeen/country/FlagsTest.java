package travel.placesbeen.country;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlagsTest {

    @Test
    void fromIsoCode_returnsFlagEmoji() {
        assertThat(Flags.fromIsoCode("DE")).isEqualTo("🇩🇪");
        assertThat(Flags.fromIsoCode("ES")).isEqualTo("🇪🇸");
        assertThat(Flags.fromIsoCode("VE")).isEqualTo("🇻🇪");
    }

    @Test
    void fromIsoCode_returnsTwoRegionalIndicatorCodePoints() {
        assertThat(Flags.fromIsoCode("AZ").codePoints().toArray()).containsExactly(0x1F1E6, 0x1F1FF);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "D", "DEU", "de", "D1"})
    void fromIsoCode_rejectsInvalidCodes(String isoCode) {
        assertThatThrownBy(() -> Flags.fromIsoCode(isoCode))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
