BUILD_ONLY_IF(OS_LINUX OS_DARWIN)
IF (OS_LINUX)
    DATA(
		# PostgreSQL bin 13.5.0 for linux
        sbr://2693545078
    )
    PEERDIR(
        contrib/libs/libpq
    )
ELSEIF (OS_DARWIN)
    DATA(
        # PostgreSQL bin 13.5.0 for Mac OS
        sbr://2693199232
    )
ENDIF()

DEPENDS(
  	antiadblock/postgres_local/recipe
)
