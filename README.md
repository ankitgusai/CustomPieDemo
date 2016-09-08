# CustomPieDemo

<img src="/ScreenShots/device-2016-09-08-124743.png" width="540" height="960"/>

This is a simple Pie like view. 
There are tons of readymade libs available already, but if you have something specific to implement and dont know how to start this might be helpful. 

 * This is more of a guide, the code is fairly documented and each step is well explained. key parts are,
    * Math that used to the point using circle equations
    * Click listener implemented for arch section(piece of pie)

Three are four step i have taken to build the View.
    -Measurement and calculation
    -View Draw
    -Item Click
    -Animation(this is still not implemented)
    
There are three attributes that can be used via xml    

```Java
        <attr name="pie_width" format="dimension" />
        <attr name="divider_color" format="color" />
        <attr name="debug_section_count" format="integer" />
```

The `debug_section_count` is only for android studio layout preview. to populate view at runtime use, 
```Java
        public void setData(List<Item> items)
```



