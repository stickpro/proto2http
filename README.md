# Proto2HTTP

A JetBrains IDE plugin that generates `.http` files from Protocol Buffer (`.proto`) files for testing gRPC services using the built-in HTTP Client.

## Features

- **Generate HTTP requests** from proto service definitions
- **Import support** — automatically resolves and includes types from imported proto files
- **Well-Known Types** — built-in support for `google.protobuf.Timestamp`, `Duration`, `FieldMask`, wrappers, and more
- **Bulk generation** — generate `.http` files for all proto files in a directory
- **Configurable settings** — customize host, output directory, and generation options

## Installation

### From JetBrains Marketplace
*(Coming soon)*

### Manual Installation
1. Download the latest release from [Releases](../../releases)
2. In your IDE: `Settings` → `Plugins` → `⚙️` → `Install Plugin from Disk...`
3. Select the downloaded `.zip` file and restart IDE

## Usage

### Single File Generation
1. Right-click on a `.proto` file in the Project view or Editor
2. Select **"Generate HTTP File"**
3. The `.http` file will be created and opened in the editor

### Bulk Generation
1. Right-click on a folder containing `.proto` files
2. Select **"Generate HTTP Files (Bulk)"**
3. All proto files with services will have `.http` files generated

### Settings
Configure the plugin at `Settings` → `Tools` → `Proto2HTTP`:

| Setting | Description |
|---------|-------------|
| **Default host** | gRPC server address (default: `localhost:50051`) |
| **Include comments** | Include comments from proto files in generated output |
| **Generate empty bodies** | Generate `{}` instead of example values |
| **Output directory** | Where to save `.http` files (same as proto / project root / custom) |

## Example

**Input:** `api.proto`
```protobuf
syntax = "proto3";
package example.users.v1;

service UserService {
  // Get user by ID
  rpc GetUser(GetUserRequest) returns (GetUserResponse);
}

message GetUserRequest {
  string id = 1;
}
```

**Output:** `api.http`
```http
### Generated from: api.proto
### Package: example.users.v1

@host = localhost:50051

###############################################################################
### UserService
###############################################################################

### Get user by ID
GRPC {{host}}/example.users.v1.UserService/GetUser

{
  "id": "string"
}
```

## Supported Types

### Primitive Types
`string`, `int32`, `int64`, `uint32`, `uint64`, `sint32`, `sint64`, `float`, `double`, `bool`, `bytes`

### Well-Known Types
| Type | JSON Value |
|------|------------|
| `google.protobuf.Timestamp` | `"2024-01-01T00:00:00Z"` |
| `google.protobuf.Duration` | `"3600s"` |
| `google.protobuf.Empty` | `{}` |
| `google.protobuf.FieldMask` | `"field1,field2"` |
| `google.protobuf.Struct` | `{"key": "value"}` |
| `google.protobuf.Any` | `{"@type": "...", "value": "..."}` |
| `google.protobuf.*Value` | Unwrapped primitive value |

## Compatibility

- **IDE versions:** IntelliJ IDEA, WebStorm, PyCharm, GoLand, PhpStorm, CLion, Rider 2024.1+
- **Kotlin:** 2.1.0
- **Java:** 21

## Building from Source

```bash
# Build plugin
./gradlew buildPlugin

# Run IDE with plugin
./gradlew runIde
```

The built plugin will be at `build/distributions/proto-ide-gen-*.zip`

## License

MIT
