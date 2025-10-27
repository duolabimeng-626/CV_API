from google.protobuf import timestamp_pb2 as _timestamp_pb2
from google.protobuf import struct_pb2 as _struct_pb2
from google.protobuf import any_pb2 as _any_pb2
from google.protobuf.internal import containers as _containers
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from typing import ClassVar as _ClassVar, Iterable as _Iterable, Mapping as _Mapping, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class TraceContext(_message.Message):
    __slots__ = ("trace_id", "span_id", "parent_span_id")
    TRACE_ID_FIELD_NUMBER: _ClassVar[int]
    SPAN_ID_FIELD_NUMBER: _ClassVar[int]
    PARENT_SPAN_ID_FIELD_NUMBER: _ClassVar[int]
    trace_id: str
    span_id: str
    parent_span_id: str
    def __init__(self, trace_id: _Optional[str] = ..., span_id: _Optional[str] = ..., parent_span_id: _Optional[str] = ...) -> None: ...

class TenantContext(_message.Message):
    __slots__ = ("tenant_id", "user_id", "attrs")
    class AttrsEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: str
        def __init__(self, key: _Optional[str] = ..., value: _Optional[str] = ...) -> None: ...
    TENANT_ID_FIELD_NUMBER: _ClassVar[int]
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    ATTRS_FIELD_NUMBER: _ClassVar[int]
    tenant_id: str
    user_id: str
    attrs: _containers.ScalarMap[str, str]
    def __init__(self, tenant_id: _Optional[str] = ..., user_id: _Optional[str] = ..., attrs: _Optional[_Mapping[str, str]] = ...) -> None: ...

class CustomStatus(_message.Message):
    __slots__ = ("code", "message", "details")
    CODE_FIELD_NUMBER: _ClassVar[int]
    MESSAGE_FIELD_NUMBER: _ClassVar[int]
    DETAILS_FIELD_NUMBER: _ClassVar[int]
    code: int
    message: str
    details: _containers.RepeatedCompositeFieldContainer[_any_pb2.Any]
    def __init__(self, code: _Optional[int] = ..., message: _Optional[str] = ..., details: _Optional[_Iterable[_Union[_any_pb2.Any, _Mapping]]] = ...) -> None: ...

class ModelSpec(_message.Message):
    __slots__ = ("name", "version", "tags")
    class TagsEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: str
        def __init__(self, key: _Optional[str] = ..., value: _Optional[str] = ...) -> None: ...
    NAME_FIELD_NUMBER: _ClassVar[int]
    VERSION_FIELD_NUMBER: _ClassVar[int]
    TAGS_FIELD_NUMBER: _ClassVar[int]
    name: str
    version: str
    tags: _containers.ScalarMap[str, str]
    def __init__(self, name: _Optional[str] = ..., version: _Optional[str] = ..., tags: _Optional[_Mapping[str, str]] = ...) -> None: ...

class InferenceHeader(_message.Message):
    __slots__ = ("model", "trace", "tenant", "options", "accept")
    MODEL_FIELD_NUMBER: _ClassVar[int]
    TRACE_FIELD_NUMBER: _ClassVar[int]
    TENANT_FIELD_NUMBER: _ClassVar[int]
    OPTIONS_FIELD_NUMBER: _ClassVar[int]
    ACCEPT_FIELD_NUMBER: _ClassVar[int]
    model: ModelSpec
    trace: TraceContext
    tenant: TenantContext
    options: _struct_pb2.Struct
    accept: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, model: _Optional[_Union[ModelSpec, _Mapping]] = ..., trace: _Optional[_Union[TraceContext, _Mapping]] = ..., tenant: _Optional[_Union[TenantContext, _Mapping]] = ..., options: _Optional[_Union[_struct_pb2.Struct, _Mapping]] = ..., accept: _Optional[_Iterable[str]] = ...) -> None: ...

class InputEnvelope(_message.Message):
    __slots__ = ("kind", "content_type", "message", "text", "binary", "json", "tags")
    class TagsEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: str
        def __init__(self, key: _Optional[str] = ..., value: _Optional[str] = ...) -> None: ...
    KIND_FIELD_NUMBER: _ClassVar[int]
    CONTENT_TYPE_FIELD_NUMBER: _ClassVar[int]
    MESSAGE_FIELD_NUMBER: _ClassVar[int]
    TEXT_FIELD_NUMBER: _ClassVar[int]
    BINARY_FIELD_NUMBER: _ClassVar[int]
    JSON_FIELD_NUMBER: _ClassVar[int]
    TAGS_FIELD_NUMBER: _ClassVar[int]
    kind: str
    content_type: str
    message: _any_pb2.Any
    text: str
    binary: bytes
    json: _struct_pb2.Struct
    tags: _containers.ScalarMap[str, str]
    def __init__(self, kind: _Optional[str] = ..., content_type: _Optional[str] = ..., message: _Optional[_Union[_any_pb2.Any, _Mapping]] = ..., text: _Optional[str] = ..., binary: _Optional[bytes] = ..., json: _Optional[_Union[_struct_pb2.Struct, _Mapping]] = ..., tags: _Optional[_Mapping[str, str]] = ...) -> None: ...

class ResultEnvelope(_message.Message):
    __slots__ = ("kind", "content_type", "message", "text", "binary", "json", "meta", "input_index")
    KIND_FIELD_NUMBER: _ClassVar[int]
    CONTENT_TYPE_FIELD_NUMBER: _ClassVar[int]
    MESSAGE_FIELD_NUMBER: _ClassVar[int]
    TEXT_FIELD_NUMBER: _ClassVar[int]
    BINARY_FIELD_NUMBER: _ClassVar[int]
    JSON_FIELD_NUMBER: _ClassVar[int]
    META_FIELD_NUMBER: _ClassVar[int]
    INPUT_INDEX_FIELD_NUMBER: _ClassVar[int]
    kind: str
    content_type: str
    message: _any_pb2.Any
    text: str
    binary: bytes
    json: _struct_pb2.Struct
    meta: _struct_pb2.Struct
    input_index: int
    def __init__(self, kind: _Optional[str] = ..., content_type: _Optional[str] = ..., message: _Optional[_Union[_any_pb2.Any, _Mapping]] = ..., text: _Optional[str] = ..., binary: _Optional[bytes] = ..., json: _Optional[_Union[_struct_pb2.Struct, _Mapping]] = ..., meta: _Optional[_Union[_struct_pb2.Struct, _Mapping]] = ..., input_index: _Optional[int] = ...) -> None: ...

class StreamOpen(_message.Message):
    __slots__ = ("header",)
    HEADER_FIELD_NUMBER: _ClassVar[int]
    header: InferenceHeader
    def __init__(self, header: _Optional[_Union[InferenceHeader, _Mapping]] = ...) -> None: ...

class StreamFrame(_message.Message):
    __slots__ = ("inputs", "frame_index", "ts")
    INPUTS_FIELD_NUMBER: _ClassVar[int]
    FRAME_INDEX_FIELD_NUMBER: _ClassVar[int]
    TS_FIELD_NUMBER: _ClassVar[int]
    inputs: _containers.RepeatedCompositeFieldContainer[InputEnvelope]
    frame_index: int
    ts: _timestamp_pb2.Timestamp
    def __init__(self, inputs: _Optional[_Iterable[_Union[InputEnvelope, _Mapping]]] = ..., frame_index: _Optional[int] = ..., ts: _Optional[_Union[_timestamp_pb2.Timestamp, _Mapping]] = ...) -> None: ...

class StreamClose(_message.Message):
    __slots__ = ("meta",)
    META_FIELD_NUMBER: _ClassVar[int]
    meta: _struct_pb2.Struct
    def __init__(self, meta: _Optional[_Union[_struct_pb2.Struct, _Mapping]] = ...) -> None: ...

class StreamRequest(_message.Message):
    __slots__ = ("open", "frame", "close")
    OPEN_FIELD_NUMBER: _ClassVar[int]
    FRAME_FIELD_NUMBER: _ClassVar[int]
    CLOSE_FIELD_NUMBER: _ClassVar[int]
    open: StreamOpen
    frame: StreamFrame
    close: StreamClose
    def __init__(self, open: _Optional[_Union[StreamOpen, _Mapping]] = ..., frame: _Optional[_Union[StreamFrame, _Mapping]] = ..., close: _Optional[_Union[StreamClose, _Mapping]] = ...) -> None: ...

class StreamAck(_message.Message):
    __slots__ = ("status",)
    STATUS_FIELD_NUMBER: _ClassVar[int]
    status: CustomStatus
    def __init__(self, status: _Optional[_Union[CustomStatus, _Mapping]] = ...) -> None: ...

class FrameResult(_message.Message):
    __slots__ = ("frame_index", "results", "meta")
    FRAME_INDEX_FIELD_NUMBER: _ClassVar[int]
    RESULTS_FIELD_NUMBER: _ClassVar[int]
    META_FIELD_NUMBER: _ClassVar[int]
    frame_index: int
    results: _containers.RepeatedCompositeFieldContainer[ResultEnvelope]
    meta: _struct_pb2.Struct
    def __init__(self, frame_index: _Optional[int] = ..., results: _Optional[_Iterable[_Union[ResultEnvelope, _Mapping]]] = ..., meta: _Optional[_Union[_struct_pb2.Struct, _Mapping]] = ...) -> None: ...

class StreamResponse(_message.Message):
    __slots__ = ("ack", "frame")
    ACK_FIELD_NUMBER: _ClassVar[int]
    FRAME_FIELD_NUMBER: _ClassVar[int]
    ack: StreamAck
    frame: FrameResult
    def __init__(self, ack: _Optional[_Union[StreamAck, _Mapping]] = ..., frame: _Optional[_Union[FrameResult, _Mapping]] = ...) -> None: ...
