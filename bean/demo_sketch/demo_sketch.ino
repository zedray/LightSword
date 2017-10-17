#define BUFFER_SIZE 64

void setup() {
  // Set a timeout so that Bean won't stall indefinitely if something goes wrong
  Serial.setTimeout(25);

  // Configure digital pins as inputs
  pinMode(0, INPUT_PULLUP);
  pinMode(1, INPUT_PULLUP);
  pinMode(2, INPUT_PULLUP);
  pinMode(3, INPUT_PULLUP);
  pinMode(4, INPUT_PULLUP);
  pinMode(5, INPUT_PULLUP);
}

void loop() {
  // Grab up to 64 bytes from Bean's Virtual Serial port
  char buffer[BUFFER_SIZE];
  uint8_t bytes_rcvd = Serial.readBytes(buffer, BUFFER_SIZE);

  if (bytes_rcvd == 1 && buffer[0] == 0x02) {
    // We got a request for data from LightBlue!

    // Read analog pins A0 and A1
    int analog1 = analogRead(A0);
    int analog2 = analogRead(A1);

    // Read digital pins D0 through D5 and join their bits into a single byte
    uint8_t digitalAll = 0;
    digitalAll |= digitalRead(0);
    digitalAll |= digitalRead(1) << 1;
    digitalAll |= digitalRead(1) << 2;
    digitalAll |= digitalRead(1) << 3;
    digitalAll |= digitalRead(1) << 4;
    digitalAll |= digitalRead(1) << 5;

    // Package the data into a 6-byte buffer
    buffer[0] = 0x82;
    buffer[1] = digitalAll;
    buffer[2] = analog1 & 0xFF;
    buffer[3] = analog1 >> 8;
    buffer[4] = analog2 & 0xFF;
    buffer[5] = analog2 >> 8;

    // Send back 6 bytes of data to LightBlue
    Serial.write((uint8_t *)buffer, 6);
  }

  // Sleep until another serial request wakes us up
  Bean.sleep(0xFFFFFFFF);
}
