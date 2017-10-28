#include <Servo.h>

// Reads from Bluetooth.
#define SCRATCH 1

// Sends 2.6v 18mA into Digital Pin 0
#define PIN_BLUE 1
#define PIN_RED 3
#define PIN_GREEN 5

#define POWER_MIN 0
#define POWER_MAX 255

void setup() {
    // Set pin mode.
    pinMode(PIN_RED, OUTPUT);
    pinMode(PIN_GREEN, OUTPUT);
    pinMode(PIN_BLUE, OUTPUT);

    // Initialize Bluetooth.
    Bean.setScratchNumber(SCRATCH, POWER_MIN);

    // Show a boot animation.
    handleCommandSetLEDs(0, 255, 255);
    handleCommandSetLEDs(255, 0, 255);
    handleCommandSetLEDs(255, 255, 0);
    handleCommandSetLEDs(0, 0, 0);
}

void loop() {
    uint32_t value = Bean.readScratchNumber(SCRATCH);
    uint8_t red = (uint8_t) value;
    uint8_t green =  (uint8_t) (value >>= 8);
    uint8_t blue =  (uint8_t) (value >>= 8);
    analogWrite(PIN_RED, red);
    analogWrite(PIN_GREEN, green);
    analogWrite(PIN_BLUE, blue);
    Bean.setLed(red, green, blue);
    Bean.sleep(100);
}

void handleCommandSetLEDs(uint8_t red, uint8_t green, uint8_t blue) {
  Bean.setLed(red, green, blue);
  analogWrite(PIN_RED, red);
  analogWrite(PIN_GREEN, green);
  analogWrite(PIN_BLUE, blue);
  Bean.sleep(500);
}
