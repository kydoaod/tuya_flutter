import 'package:flutter/material.dart';
import 'package:tuya_flutter/tuya_flutter.dart';

class TuyaDemoPage extends StatefulWidget {
  const TuyaDemoPage({Key? key}) : super(key: key);

  @override
  _TuyaDemoPageState createState() => _TuyaDemoPageState();
}

class _TuyaDemoPageState extends State<TuyaDemoPage> {
  final _emailCtrl = TextEditingController();
  final _passCtrl = TextEditingController();
  String? _homeId;
  String _status = 'Idle';
  List<Map<String, dynamic>> _foundDevices = [];

  @override
  void dispose() {
    _emailCtrl.dispose();
    _passCtrl.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    setState(() => _status = 'Logging in…');
    try {
      final res = await TuyaFlutter.loginWithEmail(
        countryCode: '63',
        email: _emailCtrl.text,
        passwd: _passCtrl.text,
      );
      setState(() => _status = 'Login: $res');
    } catch (e) {
      setState(() => _status = 'Login error: $e');
    }
  }

  Future<void> _createHome() async {
    setState(() => _status = 'Creating home…');
    try {
      final id = await TuyaFlutter.createHome(
        name: 'My Flutter Home',
        lon: 121.0,
        lat: 14.6,
        geoName: 'Manila',
        rooms: ['Living', 'Bedroom'],
      );
      setState(() {
        _homeId = id?.toString();
        _status = 'Home created: $_homeId';
      });
    } catch (e) {
      setState(() => _status = 'Create home error: $e');
    }
  }

  Future<void> _startPairing() async {
    if (_homeId == null) return;
    setState(() {
      _status = 'Getting token…';
      _foundDevices.clear();
    });

    try {
      final token = await TuyaFlutter.getActivatorToken(homeId: int.parse(_homeId!));
      setState(() => _status = 'Building activator…');
      await TuyaFlutter.buildActivator(
        token: token ?? '',
        timeout: 60,
        ssid: 'YOUR_SSID',
        password: 'YOUR_PASS',
      );
      setState(() => _status = 'Starting pairing…');
      await TuyaFlutter.startActivator();
      // Listen for events via EventChannel elsewhere
    } catch (e) {
      setState(() => _status = 'Pairing error: $e');
    }
  }

  Future<void> _stopPairing() async {
    setState(() => _status = 'Stopping pairing…');
    await TuyaFlutter.stopActivator();
    setState(() => _status = 'Pairing stopped');
  }

  Future<void> _sendDp() async {
    if (_foundDevices.isEmpty) return;
    final dev = _foundDevices.first['devId'] as String;
    setState(() => _status = 'Sending DP to $dev…');
    try {
      await TuyaFlutter.sendDpCommand(
        devId: dev,
        dpId: '1',
        dpValue: 1,
      );
      setState(() => _status = 'DP command sent');
    } catch (e) {
      setState(() => _status = 'DP error: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Tuya SDK Demo')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Login
            TextField(
              controller: _emailCtrl,
              decoration: const InputDecoration(labelText: 'Email'),
            ),
            TextField(
              controller: _passCtrl,
              decoration: const InputDecoration(labelText: 'Password'),
              obscureText: true,
            ),
            ElevatedButton(onPressed: _login, child: const Text('Login')),
            const SizedBox(height: 16),

            // Create Home
            ElevatedButton(onPressed: _createHome, child: const Text('Create Home')),
            const SizedBox(height: 16),

            // Pairing controls
            ElevatedButton(onPressed: _startPairing, child: const Text('Start Pairing')),
            ElevatedButton(onPressed: _stopPairing, child: const Text('Stop Pairing')),
            const SizedBox(height: 16),

            // Send DP
            ElevatedButton(onPressed: _sendDp, child: const Text('Send DP Command')),

            const Divider(),
            Text('Status: $_status', style: const TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            if (_foundDevices.isNotEmpty) ...[
              const Text('Found Devices:', style: TextStyle(fontWeight: FontWeight.bold)),
              for (var d in _foundDevices)
                Text('${d['device'] ?? 'Unknown'} (ID: ${d['devId']})'),
            ],
          ],
        ),
      ),
    );
  }
}
