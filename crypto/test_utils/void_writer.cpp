#include "void_writer.h"

using namespace NCrypta;

TVoidWriter::TVoidWriter(size_t streamCount)
    : StreamCount(streamCount) {
}

size_t TVoidWriter::GetTableCount() const {
    return StreamCount;
}

void TVoidWriter::FinishTable(size_t) {
}

void TVoidWriter::AddRow(const NYT::TYaMRRow&, size_t) {
}

void TVoidWriter::AddRow(NYT::TYaMRRow&&, size_t) {
}
