/*
Copyright (C) 2017 Jan K. Szymanski
This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_database/firebase_database.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:google_sign_in/google_sign_in.dart';

import 'dart:async';
import 'dart:collection';
import 'dart:math';

const List<Color> COLORS_USED = const <Color>[
  Colors.red,
  Colors.orange,
  Colors.yellow,
  Colors.green,
  Colors.cyan,
  Colors.blue,
  Colors.purple,
  Colors.pink
];

const List<String> COLOR_NAMES = const <String>[
  'RED', 'ORANGE', 'YELLOW', 'GREEN', 'CYAN', 'BLUE', 'PURPLE', 'PINK'
];

final Map<String, Color> kColorMappings = new HashMap.fromIterables(
  COLOR_NAMES,
  COLORS_USED
);

final ThemeData kIOSTheme = new ThemeData(
  primarySwatch: Colors.orange,
  primaryColor: Colors.grey[100],
  primaryColorBrightness: Brightness.light,
);

final ThemeData kDefaultTheme = new ThemeData(
  primarySwatch: Colors.purple,
  accentColor: Colors.orangeAccent[400],
);

void main() {
  runApp(new MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      title: 'Color Observer',
      theme: defaultTargetPlatform == TargetPlatform.iOS
      ? kIOSTheme
      : kDefaultTheme,
      home: new MyHomePage(title: 'Color Observer'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key, this.title}) : super(key: key);

  final String title;

  @override
  _MyHomePageState createState() => new _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  List<ColorEntry> _entries = [];

  GoogleSignIn _googleSignIn = new GoogleSignIn();
  FirebaseAuth _auth = FirebaseAuth.instance;
  FirebaseMessaging _firebaseMessaging = new FirebaseMessaging();
  DatabaseReference _reference = FirebaseDatabase.instance.reference()
        .child('logs');

  Future<Null> _ensureLoggedIn() async {
    GoogleSignInAccount user = _googleSignIn.currentUser;
    if (user == null) {
      user = await _googleSignIn.signInSilently();
    }
    if (user == null) {
      await _googleSignIn.signIn();
    }
    if (await _auth.currentUser() == null) {
      GoogleSignInAuthentication credentials =
      await _googleSignIn.currentUser.authentication;
      await _auth.signInWithGoogle(
        idToken: credentials.idToken,
        accessToken: credentials.accessToken,
      );
    }
  }

  @override
  void initState() {
    super.initState();

    FirebaseDatabase.instance.setPersistenceEnabled(true);
    FirebaseDatabase.instance.setPersistenceCacheSizeBytes(10000000);
    _reference.keepSynced(true);

    _firebaseMessaging.requestNotificationPermissions(
      const IosNotificationSettings(sound: true, badge: true, alert: true)
    );

    _ensureLoggedIn().then((user) {
      _reference.onChildAdded.listen((Event event) {
        var val = event.snapshot.value;
        var colorEntry = new ColorEntry(
          hue: val['hue'],
          saturation: val['saturation'],
          intensity: val['intensity'],
        );
        setState(() {
          _entries.add(colorEntry);
        });
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return new Scaffold(
      appBar: new AppBar(
        title: new Text(widget.title),
        elevation:
            Theme.of(context).platform == TargetPlatform.iOS ? 0.0 : 4.0,
      ),
      body: new Container(
        child: new Column(
          children: [
            new Flexible(
              child: new ListView.builder(
                padding: new EdgeInsets.all(8.0),
                itemBuilder: (_, int index) =>
                  new ColorEntryListItem(_entries[index]),
                itemCount: _entries.length,
              ),
            ),
          ]
        ),
        decoration: Theme.of(context).platform == TargetPlatform.iOS
        ? new BoxDecoration(
          border: new Border(
            top: new BorderSide(color: Colors.grey[200])
          )
        )
        : null),
    );
  }
}

class ColorEntry {
  ColorEntry(
    {this.hue,
    this.saturation,
    this.intensity}
  );
  final double hue;
  final double saturation;
  final double intensity;

  double _hueInDeg() => hue * (360 / (2*PI));

  String get hsvColor {
    num color = _hueInDeg();
    if (color >= 345 || color < 25) {
      return 'RED';
    } else if (color >= 25 && color < 50) {
      return 'ORANGE';
    } else if (color >= 50 && color < 85) {
      return 'YELLOW';
    } else if (color >= 85 && color < 165) {
      return 'GREEN';
    } else if (color >= 165 && color < 195) {
      return 'CYAN';
    } else if (color >= 195 && color < 265) {
      return 'BLUE';
    } else if (color >= 265 && color < 295) {
      return 'PURPLE';
    } else {
      return 'PINK';
    }
  }
}

class ColorEntryListItem extends StatelessWidget {
  ColorEntryListItem(this.entry);

  final ColorEntry entry;

  Widget build(BuildContext context) {
    return new Padding(
      padding: new EdgeInsets.all(8.0),
      child: new Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          new Text(entry.hue.toString()),
          new Text(entry.saturation.toString()),
          new Text(entry.intensity.toString()),
          new Text(
            entry.hsvColor,
            style: new TextStyle(
              fontWeight: FontWeight.bold,
              color: kColorMappings[entry.hsvColor],
            ),
          ),
        ]
      ),
    );
  }
}
