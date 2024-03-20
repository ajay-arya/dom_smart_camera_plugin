import 'package:clipboard/clipboard.dart';
import 'package:dom_camera/dom_camera.dart';
import 'package:flutter/material.dart';

class AlarmConfigMotion extends StatefulWidget {
  final String cameraId;

  const AlarmConfigMotion({required this.cameraId, Key? key}) : super(key: key);

  @override
  State<AlarmConfigMotion> createState() => _AlarmConfigMotionState();
}

class _AlarmConfigMotionState extends State<AlarmConfigMotion> {
  late String cameraId;
  final _domCameraPlugin = DomCamera();

  bool isInitLoading = false;
  bool isSubmitting = false;
  String currentConfig = "";
  bool isEditing = false;

  bool canUpdate = false;

  final TextEditingController _newNameController = TextEditingController();

  @override
  void initState() {
    super.initState();
    cameraId = widget.cameraId;

    getConfig();
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (!isEditing) const Text('Current Configuration:'),
          if (!isEditing)
            Row(
              children: [
                Expanded(child: Text(currentConfig)),
                IconButton(
                  onPressed: () {
                    FlutterClipboard.copy(currentConfig).then((value) =>
                        ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text("Copied!"))));
                  },
                  icon: const Icon(Icons.copy),
                  tooltip: 'Copy to Clipboard',
                ),
              ],
            ),
          if (isEditing)
            TextFormField(
              controller: _newNameController,
              decoration: const InputDecoration(labelText: 'New Config JSON'),
            ),
          const SizedBox(height: 16),
          if (!isEditing)
            ElevatedButton(
              onPressed: startEditing,
              child: const Text('Edit Camera Name'),
            )
          else
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                ElevatedButton(
                  onPressed: cancelEditing,
                  child: const Text('Cancel'),
                ),
                ElevatedButton(
                  onPressed: submitNewName,
                  child: isSubmitting
                      ? const CircularProgressIndicator()
                      : const Text('Submit'),
                ),
              ],
            ),
        ],
      ),
    );
  }

  void startEditing() {
    setState(() {
      isEditing = true;
    });
  }

  void cancelEditing() {
    setState(() {
      isEditing = false;
    });
  }

  void submitNewName() async {
    if (!canUpdate) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
              content: Text(
                  "Cannot perform SET operation without getting the details")),
        );
      }
      return;
    }

    setState(() {
      isSubmitting = true;
    });

    await _domCameraPlugin.setMoveDetectConfig(_newNameController.text);

    cancelEditing();
    setState(() {
      isSubmitting = false;
      isInitLoading = false;
    });
    getConfig();
  }

  void getConfig() async {
    setState(() {
      isInitLoading = true;
    });

    final data = await _domCameraPlugin.getMoveDetectConfig();

    if (data["isError"]) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(data["message"])),
        );
      }
      setState(() {
        isInitLoading = false;
      });
    } else {
      setState(() {
        currentConfig = data["details"];
        canUpdate = true;
        isInitLoading = false;
      });
    }
  }

  @override
  void dispose() {
    _newNameController.dispose();
    super.dispose();
  }
}
