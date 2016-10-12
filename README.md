
# cordova-plugin-sms #

Plugin to operate SMS, send / list / intercept / delete / restore.

### How to Use? ###

Use the plugin with Cordova CLI (v5.x or above):
```bash
cordova plugin add cordova-plugin-sms
```

When use with PhoneGap Build, write following line in your config.xml:
```xml
<gap:plugin name="cordova-plugin-sms" source="npm" />
```

# API Overview #

### Methods ###

```javascript
sendSMS(address(s), text, successCallback, failureCallback);
listSMS(filter, successCallback, failureCallback);
deleteSMS(filter, successCallback, failureCallback);

startWatch(successCallback, failureCallback);
stopWatch(successCallback, failureCallback);

enableIntercept(on_off, successCallback, failureCallback);
restoreSMS(msg_or_msgs, successCallback, failureCallback);

setOptions(options, successCallback, failureCallback);
```

### Events ###

```javascript
'onSMSArrive'
```

### Quick Start ###

```bash
	# create a demo project
    cordova create test1 com.rjfun.test1 Test1
    cd test1
    cordova platform add android
    
    # now add plugin
    cordova plugin add cordova-plugin-sms
    
    # copy the demo file
    rm -r www/*; cp plugins/cordova-plugin-sms/test/* www/;
    
	# now build and run the demo in your device or emulator
    cordova prepare; 
    cordova run android; 
    
    # or import into Xcode / eclipse
```

### Documentation ###

Check the [API Reference](https://github.com/floatinghotpot/cordova-plugin-sms/blob/master/docs/)

Check the [Example Code in test/index.html](https://github.com/floatinghotpot/cordova-plugin-sms/blob/master/test/index.html).

### Demo ###

![ScreenShot](docs/sms.jpg)

### Example ###

html

	<ion-view view-title="Account" ng-controller="AccountCtrl">
	  <ion-content>
	    <ion-list>

	      <button ng-click="enviar()">enviar HOLA</button>

	    </ion-list>
	  </ion-content>
	</ion-view>

javascript file

    .controller('AccountCtrl', function ($scope) {
  
    document.addEventListener("deviceready", aba, false);

    document.addEventListener('onSMSArrive', function (mensaje) {
      var sms = mensaje.data;
      console.log("el cuerpo del mensaje interceptado es: " + JSON.stringify(sms['body']));
      $scope.listar();

    });
    
    function aba() {

      //necesario invocar esta funcion antes de el onsmsarrive
      if (SMS) SMS.startWatch(function () {
        //$scope.listar();

        console.log("watching");
        interceptar();
      }, function () {
      });

    }

    function interceptar() {
    
      if (SMS) SMS.enableIntercept(true, function () {

        console.log("interceptar activado");

      }, function () {
      });
    }

    var filter = {
      box: 'inbox', // 'inbox' (default), 'sent', 'draft', 'outbox', 'failed', 'queued', and '' for all

      // following 4 filters should NOT be used together, they are OR relationship
      read: 0, // 0 for unread SMS, 1 for SMS already read
      //_id : 1234, // specify the msg id
      //address: '+3413403677', // sender's phone number

      //este solo funciona si tiene exactamente ese texto
      //body: 'Ukx', // content to match

      // following 2 filters can be used to list page up/down
      indexFrom: 0, // start from index 0
      maxCount: 20 // count of SMS to return each time
    };

    $scope.listar = function () {

      //lista un arreglo con mensajes que cumple la condicion del filtro.
      // NO INCLUYE EL ULTIMO MENSAJE!!!
      
      if (SMS) SMS.listSMS(filter, function (data) {

        for (var i = 0; i < data.length; i++) {
          console.log("indice:" + i + " mensaje:" + data[i]['body']);
        }


      }, function (err) {
        console.log("error" + err);
      });
    };

    function envioExitoso() {
      console.log("envio exitoso");
    }

    function envioErroneo(e) {
      console.log("envio erroneo" + e);
    }

    $scope.enviar = function () {
      if (SMS)
        SMS.sendSMS("+5493413403677", "hola", envioExitoso, envioErroneo);
    };

  }
)
;


### Credits ###

The plugin is created and maintained by Raymond Xie.

You can use it for FREE, it works in trial mode by default.

[A valid license](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=86JSRPJDQUMRU) is required to get email support, or use it in commercial product.

## See Also ##

More Cordova/PhoneGap plugins by Raymond Xie, [visit http://rjfun.github.io/](http://rjfun.github.io/).

Cordova/PhoneGap plugins for the world leading Mobile Ad services:

* [AdMob PluginPro](https://github.com/floatinghotpot/cordova-admob-pro), for Google AdMob/DoubleClick.
* [iAd PluginPro](https://github.com/floatinghotpot/cordova-plugin-iad), for Apple iAd. 
* [FacebookAds PluginPro](https://github.com/floatinghotpot/cordova-plugin-facebookads), for Facebook Audience Network.
* [FlurryAds PluginPro](https://github.com/floatinghotpot/cordova-plugin-flurry), for Flurry Ads.
* [mMedia PluginPro](https://github.com/floatinghotpot/cordova-plugin-mmedia), for Millennial Meida.
* [MobFox PluginPro](https://github.com/floatinghotpot/cordova-mobfox-pro), for MobFox.
* [MoPub PluginPro](https://github.com/floatinghotpot/cordova-plugin-mopub), for MoPub.

Project outsourcing and consulting service is also available. Please [contact us](http://floatinghotpot.github.io) if you have the business needs.

