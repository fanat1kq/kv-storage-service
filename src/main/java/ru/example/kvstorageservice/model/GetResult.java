package ru.example.kvstorageservice.model;

public record GetResult(boolean found, byte[] value) {
}