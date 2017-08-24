#include <HX711ADC.h>
#include "Adafruit_SSD1306.h"

#define OLED_RESET D4
Adafruit_SSD1306 display(OLED_RESET);

#define LOGO16_GLCD_HEIGHT 16
#define LOGO16_GLCD_WIDTH  16
#if (SSD1306_LCDHEIGHT != 64)
#error("Height incorrect, please fix Adafruit_SSD1306.h!");
#endif

//HX711 Pin Hookup
#define SCK A3
#define DT A2

//Initialize scale
HX711ADC scale(DT,SCK);

double prevScaleValue;
double scaleValue;
double fullWeight = 100;
double lockWeight;
bool locked = false;
int lockedInt = 0;

bool updatePrev = true;
double seenWeight;
int numTimesSeen;

int tareScale(String command);
double absValue(double value);
int setFullWeight(String command);
int lockScale(String command);

void setup(){
  Serial.begin(9600);
  Particle.variable("scaleValue", scaleValue);
  Particle.variable("lockedInt", lockedInt);
  Particle.function("setFull", setFullWeight);
  Particle.function("tareScale", tareScale);
  Particle.function("lockScale", lockScale);

  display.begin(SSD1306_SWITCHCAPVCC, 0x3C);
  display.display();

  scale.set_scale(107.32);//Calibrates scale (See calibration instructions)

  Serial.println(scale.get_units());
  scale.tare();
}

void loop(){
  scale.power_up();

  //Check for a change in weight and send event
  if(scaleValue > prevScaleValue+2 || scaleValue < prevScaleValue - 2){
    Particle.publish("weightChange");
  }

  if(locked && (scaleValue > prevScaleValue +2 || scaleValue < prevScaleValue -2)){
    updatePrev = false;
    numTimesSeen++;
    if(numTimesSeen >=2){
      updatePrev = true;
      numTimesSeen = 0;
      Particle.publish("lockWtChange");
    }
  }

  //Get new Readings
  if(updatePrev){
    prevScaleValue = scaleValue;
  }
  scaleValue = scale.get_units(5);
  scaleValue = absValue(scaleValue);

  //Update Display
  display.clearDisplay();
  display.setTextSize(2);
  display.setTextColor(WHITE);
  display.setCursor(0,0);
  display.printf("Wt:%2d g", (int)scaleValue);
  display.setTextSize(1);
  display.printf("\n\nFull: %2d g", (int)fullWeight);
  if(locked){
    display.printf("\n\n\nLocked at %2d g", (int)lockWeight);
  }
  display.display();


  //Serial.print(scaleValue);Serial.println("g");

  scale.power_down();
  //delay(500);
  int startTime = millis();
  while(true){
    if(millis()-startTime >= 500){
      break;
    }
  }

}


int tareScale(String command){
  scale.tare();
  delay(100);
  Serial.println("Scale Teared From Cloud!!");
  return 1;
}

int setFullWeight(String command){
  fullWeight = scaleValue;
  return fullWeight;
}

int lockScale(String command){
  locked = !locked;
  lockWeight = scaleValue;

  if(locked){
    lockedInt = 1;
    return lockWeight;
  }else{
    lockedInt = 0;
    return -1;
  }
}

//Not really working right now
double absValue(double value){
  double newValue = 0;
  if(value < 0){
    newValue = -1*value;
    return newValue;
  }
  return value;
}
